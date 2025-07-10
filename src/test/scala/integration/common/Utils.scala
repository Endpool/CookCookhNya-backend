package integration.common

import api.Authentication.AuthenticatedUser
import api.ingredients.CreateIngredientReqBody
import api.users.CreateUserReqBody
import db.DbError
import db.repositories.{IngredientsRepo, RecipeIngredientsRepo, RecipesRepo, StorageIngredientsRepo, StoragesRepo}
import domain.{IngredientId, InternalServerError, RecipeId, StorageId, UserId}

import io.circe.Encoder
import io.circe.generic.auto.deriveEncoder
import io.circe.parser.decode
import io.circe.syntax.*
import zio.{RIO, UIO, ZIO, ZLayer}
import zio.http.{Path, URL, Body, Client, Header, MediaType, Request}
import zio.test.Gen

object Utils:
  extension(req: Request)
    def addAuthorization(authorizedUser: AuthenticatedUser): Request =
      req.addHeader(Header.Authorization.Bearer(authorizedUser.userId.toString))

    def withJsonBody[A](value: A)(using encoder: Encoder[A]): Request =
      req.addHeader(Header.ContentType(MediaType.application.json))
        .withBody(Body.fromCharSequence(value.asJson.toString))

  extension[A](seq1: Seq[A])
    def hasSameElementsAs(seq2: Seq[A])(using ord: Ordering[A]): Boolean
      =  seq1.length == seq2.length // for optimization
      && seq1.sorted == seq2.sorted

  extension[R, E, A](zio: ZIO[AuthenticatedUser & R, E, A])
    def provideUser(user: AuthenticatedUser): ZIO[R, E, A] =
      zio.provideSomeLayer(ZLayer.succeed(user))

  // redefining here for the sake of having default value of body
  def put(url: URL, body: Body = Body.empty): Request = Request.put(url, body)
  def post(url: URL, body: Body = Body.empty): Request = Request.post(url, body)

  def registerUser: RIO[Client, AuthenticatedUser] =
    Gen.long(1, 100000000)
      .runHead.someOrElse(52L)
      .flatMap(registerUser)

  def registerNUsers(n: Int): RIO[Client, Vector[AuthenticatedUser]] =
    ZIO.foreach((1 to n).toVector)(_ => registerUser)

  def registerUser(userId: UserId): RIO[Client, AuthenticatedUser] = for
    alias <- Gen.alphaNumericStringBounded(3, 13).runHead
    fullName <- Gen.alphaNumericStringBounded(3, 13).runHead.someOrElse("fullName")
    authUser <- registerUser(userId, alias, fullName)
  yield authUser

  def registerUser(
    userId: UserId,
    alias: Option[String],
    fullName: String,
  ): RIO[Client, AuthenticatedUser] =
    val authUser = AuthenticatedUser.createFromUserId(userId)
    for
      _ <- Client.batched(
        put(URL(Path.root / "users"))
          .withJsonBody(CreateUserReqBody(alias, fullName))
          .addAuthorization(authUser)
      )
    yield authUser

  def randomString: UIO[String] =
    Gen
    .stringBounded(5, 30)(Gen.alphaNumericChar)
    .runHead
    .someOrElse("randomString")

  def createIngredient: ZIO[IngredientsRepo, InternalServerError, IngredientId] =
    randomString.flatMap(name =>
      ZIO.serviceWithZIO[IngredientsRepo] {
        _.add(name).map(_.id)
      }.mapError(_ => InternalServerError())
    )

  def createNIngredients(n: Int): ZIO[IngredientsRepo, InternalServerError, Vector[IngredientId]] =
    ZIO.collectAll(
      (1 to n).map(_ => createIngredient).toVector
    )

  def createRecipe(ingredientIds: Vector[IngredientId]): ZIO[
    RecipesRepo & IngredientsRepo & RecipeIngredientsRepo,
    InternalServerError | DbError,
    RecipeId
  ] =
    for
      name <- randomString
      link <- randomString
      recipeId <- ZIO.serviceWithZIO[RecipesRepo](
        _.addRecipe(name, link, ingredientIds)
      )
    yield recipeId

  def createStorage(authenticatedUser: AuthenticatedUser): ZIO[StoragesRepo, DbError, StorageId] =
    for
      name <- randomString
      storageId <- ZIO.serviceWithZIO[StoragesRepo](_.createEmpty(name))
        .provideSomeLayer(ZLayer.succeed(authenticatedUser))
    yield storageId

  def addIngredientsToStorage(storageId: StorageId, ingredientIds: Vector[IngredientId]):
    ZIO[StorageIngredientsRepo, DbError, Unit] =
    ZIO.serviceWithZIO[StorageIngredientsRepo](repo =>
      ZIO.foreachDiscard(ingredientIds)(repo.addIngredientToStorage(storageId, _))
    )

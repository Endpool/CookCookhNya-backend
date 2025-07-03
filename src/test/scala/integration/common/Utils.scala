package integration.common

import api.ingredients.CreateIngredientReqBody
import api.users.CreateUserReqBody
import db.DbError
import db.repositories.{IngredientsRepo, RecipeIngredientsRepo, RecipesRepo, StorageIngredientsRepo, StoragesRepo}
import domain.{IngredientId, InternalServerError, RecipeId, StorageId, UserId}

import io.circe.Encoder
import io.circe.generic.auto.deriveEncoder
import io.circe.parser.decode
import io.circe.syntax.*
import zio.{RIO, UIO, ZIO}
import zio.http.{Body, Client, Header, MediaType, Request}
import zio.test.Gen

object Utils:
  extension(req: Request)
    def addAuthorization(userId: UserId): Request =
      req.addHeader(Header.Authorization.Bearer(userId.toString))

    def withJsonBody[A](value: A)(using encoder: Encoder[A]): Request =
      req.addHeader(Header.ContentType(MediaType.application.json))
        .withBody(Body.fromCharSequence(value.asJson.toString))

  extension[A](seq1: Seq[A])
    def hasSameElementsAs(seq2: Seq[A])(using ord: Ordering[A]): Boolean
      =  seq1.length == seq2.length // for optimization
      && seq1.sorted == seq2.sorted

  // redefining here for the sake of having default value of body
  def put(url: String, body: Body = Body.empty): Request = Request.put(url, body)
  def post(url: String, body: Body = Body.empty): Request = Request.post(url, body)

  def registerUser: RIO[Client, UserId] =
    Gen.long(1, 100000000)
      .runHead.someOrElse(52L)
      .flatMap(registerUser)

  def registerNUsers(n: Int): RIO[Client, Vector[UserId]] =
    ZIO.foreach((1 to n).toVector)(_ => registerUser)

  def registerUser(userId: UserId): RIO[Client, UserId] = for
    alias <- Gen.alphaNumericStringBounded(3, 13).runHead
    fullName <- Gen.alphaNumericStringBounded(3, 13).runHead.someOrElse("fullName")
    resp <- Client.batched(
      put("users")
        .withJsonBody(CreateUserReqBody(alias, fullName))
        .addAuthorization(userId)
    )
    _ <- resp.body.asString
  yield userId

  def registerUser(
    userId: UserId,
    alias: Option[String],
    fullName: String,
  ): RIO[Client, UserId] = for
    resp <- Client.batched(
      put("users")
        .withJsonBody(CreateUserReqBody(alias, fullName))
        .addAuthorization(userId)
    )
  yield userId

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

  def createStorage(ownerId: UserId): ZIO[StoragesRepo, DbError, StorageId] =
    for
      name <- randomString
      storageId <- ZIO.serviceWithZIO[StoragesRepo](_.createEmpty(name, ownerId))
    yield storageId

  def addIngredientsToStorage(storageId: StorageId, ingredientIds: Vector[IngredientId]):
    ZIO[StorageIngredientsRepo, DbError, Unit] =
    ZIO.foreach(ingredientIds)
      (id => ZIO.serviceWithZIO[StorageIngredientsRepo](_.addIngredientToStorage(storageId, id)))
    *> ZIO.unit



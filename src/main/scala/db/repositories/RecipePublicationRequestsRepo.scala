package db.repositories

import api.Authentication.AuthenticatedUser
import db.DbError
import db.QuillConfig.ctx
import db.tables.DbRecipe
import db.tables.publication.{DbPublicationRequestStatus, DbRecipePublicationRequest}
import domain.{PublicationRequestId, PublicationRequestStatus, RecipeId, UserId}
import io.getquill.*

import java.util.UUID
import javax.sql.DataSource
import zio.{IO, RLayer, ZIO, ZLayer}

trait RecipePublicationRequestsRepo:
  def createPublicationRequest(recipeId: RecipeId): IO[DbError, PublicationRequestId]
  def getPendingRequestsWithRecipes: IO[DbError, Seq[(DbRecipePublicationRequest, DbRecipe)]]
  def get(id: PublicationRequestId): IO[DbError, Option[DbRecipePublicationRequest]]
  def getWithRecipe(id: PublicationRequestId): IO[DbError, Option[(DbRecipePublicationRequest, DbRecipe)]]
  def updateStatus(id: PublicationRequestId, status: PublicationRequestStatus):
    IO[DbError, Boolean]
  def getAllByRecipeId(recipeId: RecipeId): IO[DbError, List[DbRecipePublicationRequest]]
  def getAllCreatedBy: ZIO[AuthenticatedUser, DbError, List[DbRecipePublicationRequest]]

private inline def recipePublicationRequests = query[DbRecipePublicationRequest]

final case class RecipePublicationRequestsRepoLive(dataSource: DataSource)
  extends RecipePublicationRequestsRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import RecipePublicationRequestsQueries.*

  private given DataSource = dataSource

  override def createPublicationRequest(recipeId: RecipeId): IO[DbError, PublicationRequestId] =
    run(
      requestPublicationQ(lift(recipeId))
    ).provideDS

  override def getPendingRequestsWithRecipes:
    IO[DbError, List[(DbRecipePublicationRequest, DbRecipe)]] =
    run(
      pendingRequestsQ
        .join(RecipesQueries.recipesQ)
        .on(_.recipeId == _.id)
    ).provideDS

  override def get(id: PublicationRequestId): IO[DbError, Option[DbRecipePublicationRequest]] =
    run(getQ(lift(id)).value).provideDS

  override def getWithRecipe(id: PublicationRequestId):
    IO[DbError, Option[(DbRecipePublicationRequest, DbRecipe)]] =

    run(
      requestsQ
        .join(RecipesQueries.recipesQ)
        .on(_.recipeId == _.id)
    ).map(_.headOption).provideDS

  override def updateStatus(id: PublicationRequestId, status: PublicationRequestStatus):
    IO[DbError, Boolean] =
    val (dbStatus, reason) = DbPublicationRequestStatus.fromDomain(status)
    run(
      updateQ(lift(id), lift(dbStatus), lift(reason))
    ).map(_ > 0).provideDS

  override def getAllByRecipeId(recipeId: RecipeId): IO[DbError, List[DbRecipePublicationRequest]] =
    run(getAllByRecipeIdQ(lift(recipeId))).provideDS

  override def getAllCreatedBy: ZIO[AuthenticatedUser, DbError, List[DbRecipePublicationRequest]] = 
    for 
      userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      res <- run(getAllCreatedByQ(lift(userId))).provideDS
    yield res
  

object RecipePublicationRequestsQueries:
  inline def requestsQ: EntityQuery[DbRecipePublicationRequest] =
    query[DbRecipePublicationRequest]

  inline def requestPublicationQ(inline recipeId: RecipeId):
    ActionReturning[DbRecipePublicationRequest, UUID] =
    requestsQ
      .insert(_.recipeId -> recipeId)
      .returningGenerated(_.id)

  inline def pendingRequestsQ: EntityQuery[DbRecipePublicationRequest] =
    requestsQ
      .filter(r =>
        infix"${r.status} = 'pending'::publication_request_status"
          .asCondition
      )

  inline def pendingRequestsByRecipeIdQ(inline recipeId: RecipeId):
    EntityQuery[DbRecipePublicationRequest] =
    pendingRequestsQ
      .filter(_.recipeId == recipeId)

  inline def getQ(inline id: PublicationRequestId): EntityQuery[DbRecipePublicationRequest] =
    requestsQ
      .filter(_.id == id)

  inline def updateQ(
    inline id: PublicationRequestId,
    inline status: DbPublicationRequestStatus,
    inline reason: Option[String],
  ): Update[DbRecipePublicationRequest] =
    requestsQ
      .filter(_.id == id)
      .update(
        _.status -> status,
        _.reason -> reason,
      )

  inline def getAllByRecipeIdQ(inline recipeId: RecipeId): EntityQuery[DbRecipePublicationRequest] =
    recipePublicationRequests.filter(_.recipeId == recipeId)

  inline def getAllCreatedByQ(inline userId: UserId): Query[DbRecipePublicationRequest] =
    query[DbRecipePublicationRequest]
      .join(query[DbRecipe])
      .on(_.recipeId == _.id)
      .map(_._1)

object RecipePublicationRequestsRepo:
  def layer: RLayer[DataSource, RecipePublicationRequestsRepo] =
    ZLayer.fromFunction(RecipePublicationRequestsRepoLive.apply)

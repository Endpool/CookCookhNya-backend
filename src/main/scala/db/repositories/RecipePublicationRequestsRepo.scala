package db.repositories

import db.DbError
import db.QuillConfig.ctx
import db.tables.DbRecipe
import db.tables.publication.{DbPublicationRequestStatus, DbRecipePublicationRequest}
import domain.{PublicationRequestStatus, PublicationRequestId, RecipeId}

import io.getquill.*
import java.util.UUID
import javax.sql.DataSource
import zio.{IO, RLayer, ZLayer, ZIO}

trait RecipePublicationRequestsRepo:
  def createPublicationRequest(recipeId: RecipeId): IO[DbError, PublicationRequestId]
  def getPendingRequestsWithRecipes: IO[DbError, Seq[(DbRecipePublicationRequest, DbRecipe)]]
  def get(id: PublicationRequestId): IO[DbError, Option[DbRecipePublicationRequest]]
  def getWithRecipe(id: PublicationRequestId): IO[DbError, Option[(DbRecipePublicationRequest, DbRecipe)]]
  def updateStatus(id: PublicationRequestId, status: PublicationRequestStatus):
    IO[DbError, Boolean]

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

object RecipePublicationRequestsRepo:
  def layer: RLayer[DataSource, RecipePublicationRequestsRepo] =
    ZLayer.fromFunction(RecipePublicationRequestsRepoLive.apply)

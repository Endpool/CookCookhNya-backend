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
      createPublicationRequestQ(lift(recipeId))
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

  override def updateStatus(id: PublicationRequestId, status: PublicationRequestStatus):
    IO[DbError, Boolean] =
    val (dbStatus, reason) = DbPublicationRequestStatus.fromDomain(status)
    run(
      updateQ(lift(id), lift(dbStatus), lift(reason))
    ).map(_ > 0).provideDS

object RecipePublicationRequestsQueries:
  inline def recipePublicationRequestsQ: EntityQuery[DbRecipePublicationRequest] =
    query[DbRecipePublicationRequest]

  inline def createPublicationRequestQ(inline recipeId: RecipeId):
    ActionReturning[DbRecipePublicationRequest, UUID] =
    recipePublicationRequestsQ
      .insert(_.recipeId -> recipeId)
      .returningGenerated(_.id)

  inline def pendingRequestsQ: EntityQuery[DbRecipePublicationRequest] =
    recipePublicationRequestsQ
      .filter(r =>
        infix"${r.status} = 'pending'::publication_request_status"
          .asCondition
      )

  inline def pendingRequestsOfRecipeQ(inline recipeId: RecipeId):
    EntityQuery[DbRecipePublicationRequest] =
    pendingRequestsQ
      .filter(_.recipeId == recipeId)

  inline def getQ(inline id: PublicationRequestId): EntityQuery[DbRecipePublicationRequest] =
    recipePublicationRequestsQ
      .filter(_.id == id)

  inline def updateQ(
    inline id: PublicationRequestId,
    inline status: DbPublicationRequestStatus,
    inline reason: Option[String],
  ): Update[DbRecipePublicationRequest] =
    recipePublicationRequestsQ
      .filter(_.id == id)
      .update(
        _.status -> status,
        _.reason -> reason,
      )

object RecipePublicationRequestsRepo:
  def layer: RLayer[DataSource, RecipePublicationRequestsRepo] =
    ZLayer.fromFunction(RecipePublicationRequestsRepoLive.apply)

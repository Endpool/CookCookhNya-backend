package db.repositories

import db.DbError
import db.QuillConfig.ctx
import db.tables.DbRecipe
import db.tables.publication.{DbPublicationRequestStatus, DbRecipePublicationRequest}
import db.tables.publication.DbPublicationRequestStatus.Pending
import domain.{PublicationRequestStatus, PublicationRequestId, RecipeId}

import io.getquill.*
import javax.sql.DataSource
import zio.{IO, RLayer, ZLayer, ZIO}
import java.util.UUID

trait RecipePublicationRequestsRepo:
  def createPublicationRequest(recipeId: RecipeId): IO[DbError, PublicationRequestId]
  def getAllPendingIds: IO[DbError, Vector[PublicationRequestId]]
  def get(id: PublicationRequestId): IO[DbError, Option[DbRecipePublicationRequest]]
  def getWithRecipe(id: PublicationRequestId):
    IO[DbError, Option[(DbRecipePublicationRequest, DbRecipe)]]
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

  override def getAllPendingIds: IO[DbError, Vector[PublicationRequestId]] =
    run(pendingRequestsQ.map(_.id)).provideDS.map(Vector.from)

  override def get(id: PublicationRequestId): IO[DbError, Option[DbRecipePublicationRequest]] =
    run(getQ(id).value).provideDS

  override def getWithRecipe(id: PublicationRequestId): IO[DbError, Option[(DbRecipePublicationRequest, DbRecipe)]] =
    run(
      getQ(id)
        .join(RecipesQueries.recipesQ)
        .on((rpq, r) => rpq.recipeId == r.id)
        .value
    ).provideDS

  override def updateStatus(id: RecipeId, status: PublicationRequestStatus): IO[DbError, Boolean] =
    val (dbStatus, reason) = DbPublicationRequestStatus.fromDomain(status)
    run(updateQ(id, dbStatus, reason)).map(_ > 0).provideDS

object RecipePublicationRequestsQueries:
  import db.QuillConfig.ctx.*

  inline def recipePublicationRequestsQ: EntityQuery[DbRecipePublicationRequest] =
    query[DbRecipePublicationRequest]

  inline def createPublicationRequestQ(inline recipeId: RecipeId):
    ActionReturning[DbRecipePublicationRequest, UUID] =
    recipePublicationRequestsQ
      .insert(_.recipeId -> recipeId)
      .returningGenerated(_.id)

  inline def pendingRequestsQ: EntityQuery[DbRecipePublicationRequest] =
    recipePublicationRequestsQ
      .filter(_.status == lift(Pending))

  inline def pendingRequestsOfRecipeQ(inline recipeId: RecipeId):
    EntityQuery[DbRecipePublicationRequest] =
    pendingRequestsQ
      .filter(_.recipeId == recipeId)

  inline def getQ(inline id: PublicationRequestId): EntityQuery[DbRecipePublicationRequest] =
    recipePublicationRequestsQ
      .filter(_.id == lift(id))

  inline def updateQ(
    inline id: PublicationRequestId,
    inline status: DbPublicationRequestStatus,
    inline reason: Option[String],
  ): Update[DbRecipePublicationRequest] =
    recipePublicationRequestsQ
      .filter(_.id == lift(id))
      .update(
        _.status -> lift(status),
        _.reason -> lift(reason),
      )

object RecipePublicationRequestsRepo:
  def layer: RLayer[DataSource, RecipePublicationRequestsRepo] =
    ZLayer.fromFunction(RecipePublicationRequestsRepoLive.apply)

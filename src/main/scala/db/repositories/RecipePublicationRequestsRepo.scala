package db.repositories

import db.DbError
import db.QuillConfig.ctx
import db.tables.publication.DbPublicationRequestStatus.Pending
import db.tables.publication.{DbPublicationRequestStatus, DbRecipePublicationRequest}
import domain.{PublicationRequestId, RecipeId, PublicationRequestNotFound}

import io.getquill.*
import javax.sql.DataSource
import zio.{IO, RLayer, ZLayer, ZIO}

trait RecipePublicationRequestsRepo:
  def requestPublication(recipeId: RecipeId): IO[DbError, Unit]
  def getAllPending: IO[DbError, Seq[DbRecipePublicationRequest]]
  def get(id: PublicationRequestId): IO[DbError, Option[DbRecipePublicationRequest]]
  def update(id: PublicationRequestId, comment: String, status: DbPublicationRequestStatus):
    IO[DbError | PublicationRequestNotFound, Unit]

final case class RecipePublicationRequestsRepoLive(dataSource: DataSource)
  extends RecipePublicationRequestsRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import RecipePublicationRequestsQueries.*

  private given DataSource = dataSource

  override def requestPublication(recipeId: RecipeId): IO[DbError, Unit] =
    run(requestPublicationQ(lift(recipeId))).unit.provideDS

  override def getAllPending: IO[DbError, Seq[DbRecipePublicationRequest]] =
    run(allPendingQ).provideDS

  override def get(id: PublicationRequestId): IO[DbError, Option[DbRecipePublicationRequest]] =
    run(getQ(id)).map(_.headOption).provideDS

  override def update(id: RecipeId, comment: String, status: DbPublicationRequestStatus):
    IO[DbError | PublicationRequestNotFound, Unit] =

    run(updateQ(id, comment, status)).provideDS.flatMap {
      case 0 => ZIO.fail(PublicationRequestNotFound(id))
      case _ => ZIO.unit
    }

object RecipePublicationRequestsQueries:
  import db.QuillConfig.ctx.*

  inline def recipePublicationRequestsQ = query[DbRecipePublicationRequest]

  inline def requestPublicationQ(inline recipeId: RecipeId) =
    recipePublicationRequestsQ
      .insert(_.recipeId -> recipeId)

  inline def allPendingQ =
    recipePublicationRequestsQ
      .filter(_.status == lift(Pending))

  inline def pendingRequestsByIdQ(inline recipeId: RecipeId) = allPendingQ.filter(_.recipeId == recipeId)

  inline def getQ(inline id: PublicationRequestId) =
    recipePublicationRequestsQ
      .filter(_.id == lift(id)).take(1)

  inline def updateQ(inline id: PublicationRequestId, inline comment: String, inline status: DbPublicationRequestStatus) =
    recipePublicationRequestsQ
      .filter(_.id == lift(id))
      .update(
        _.comment -> lift(comment),
        _.status  -> lift(status),
      )

object RecipePublicationRequestsRepo:
  def layer: RLayer[DataSource, RecipePublicationRequestsRepo] =
    ZLayer.fromFunction(RecipePublicationRequestsRepoLive.apply)

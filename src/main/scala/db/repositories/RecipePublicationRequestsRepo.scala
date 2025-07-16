package db.repositories

import db.DbError
import db.QuillConfig.ctx
import db.tables.publication.DbPublicationRequestStatus.{Pending, createType}
import db.tables.publication.{DbPublicationRequestStatus, DbRecipePublicationRequest}
import domain.{RecipeId, PublicationRequestNotFound}
import io.getquill.*
import zio.{IO, RLayer, ZLayer, ZIO}

import java.util.UUID
import javax.sql.DataSource

trait RecipePublicationRequestsRepo:
  def requestPublication(recipeId: RecipeId): IO[DbError, Unit]
  def getAllPending: IO[DbError, Seq[DbRecipePublicationRequest]]
  def get(id: UUID): IO[DbError, Option[DbRecipePublicationRequest]]
  def update(id: UUID, comment: String, status: DbPublicationRequestStatus):
    IO[DbError | PublicationRequestNotFound, Unit]

private inline def recipePublicationRequests = query[DbRecipePublicationRequest]

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

  override def get(id: UUID): IO[DbError, Option[DbRecipePublicationRequest]] =
    run(getQ(id)).map(_.headOption).provideDS

  override def update(id: RecipeId, comment: String, status: DbPublicationRequestStatus):
    IO[DbError | PublicationRequestNotFound, Unit] =

    run(updateQ(id, comment, status)).provideDS.flatMap {
      case 0 => ZIO.fail(PublicationRequestNotFound(id.toString))
      case _ => ZIO.unit
    }

object RecipePublicationRequestsQueries:
  import db.QuillConfig.ctx.*
  
  inline def requestPublicationQ(inline recipeId: RecipeId) =
    recipePublicationRequests.insert(_.recipeId -> recipeId)

  inline def allPendingQ = recipePublicationRequests.filter(_.status == lift(Pending))

  inline def pendingRequestsByIdQ(inline recipeId: RecipeId) = allPendingQ.filter(_.recipeId == recipeId)

  inline def getQ(inline id: UUID) =
    recipePublicationRequests.filter(_.id == lift(id)).take(1)

  inline def updateQ(inline id: UUID, inline comment: String, inline status: DbPublicationRequestStatus) =
    recipePublicationRequests.filter(_.id == lift(id))
      .update(
        _.comment -> lift(comment),
        _.status  -> lift(status)
      )
  
object RecipePublicationRequestsRepo:
  def layer: RLayer[DataSource, RecipePublicationRequestsRepo] =
    ZLayer.fromFunction(RecipePublicationRequestsRepoLive.apply)

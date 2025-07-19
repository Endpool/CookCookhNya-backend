package db.repositories

import db.DbError
import db.tables.publication.{DbIngredientPublicationRequest, DbPublicationRequestStatus}
import db.tables.publication.DbPublicationRequestStatus.Pending
import domain.{PublicationRequestId, IngredientId, PublicationRequestStatus}

import io.getquill.*
import javax.sql.DataSource
import zio.{IO, RLayer, ZLayer, ZIO}
import java.util.UUID

trait IngredientPublicationRequestsRepo:
  def requestPublication(ingredientId: IngredientId): IO[DbError, PublicationRequestId]
  def getAllPending: IO[DbError, Seq[DbIngredientPublicationRequest]]
  def get(id: PublicationRequestId): IO[DbError, Option[DbIngredientPublicationRequest]]
  def updateStatus(id: PublicationRequestId, status: PublicationRequestStatus): IO[DbError, Boolean]

final case class IngredientPublicationRequestsRepoLive(dataSource: DataSource)
  extends IngredientPublicationRequestsRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import IngredientPublicationRequestsQueries.*

  private given DataSource = dataSource

  override def requestPublication(ingredientId: IngredientId): IO[DbError, PublicationRequestId] =
    run(requestPublicationQ(lift(ingredientId))).provideDS

  override def getAllPending: IO[DbError, Seq[DbIngredientPublicationRequest]] =
    run(allPendingQ).provideDS

  override def get(id: PublicationRequestId): IO[DbError, Option[DbIngredientPublicationRequest]] =
    run(getQ(id)).map(_.headOption).provideDS

  override def updateStatus(id: PublicationRequestId, status: PublicationRequestStatus):
    IO[DbError, Boolean] =
    val (dbStatus, reason) = DbPublicationRequestStatus.fromDomain(status)
    run(updateQ(id, dbStatus, reason)).map(_ > 0).provideDS

object IngredientPublicationRequestsQueries:
  import db.QuillConfig.ctx.*

  inline def ingredientPublicationRequestsQ = query[DbIngredientPublicationRequest]

  inline def requestPublicationQ(inline ingredientId: IngredientId):
    ActionReturning[DbIngredientPublicationRequest, UUID] =
    ingredientPublicationRequestsQ
      .insert(_.ingredientId -> ingredientId)
      .returningGenerated(_.id)

  inline def allPendingQ: EntityQuery[DbIngredientPublicationRequest] =
    ingredientPublicationRequestsQ
      .filter(r => infix"${r.status} = 'pending'::publication_request_status".as[Boolean])

  inline def pendingRequestsByIdQ(inline ingredientId: IngredientId) = allPendingQ.filter(_.ingredientId == ingredientId)

  inline def getQ(inline id: PublicationRequestId) =
    ingredientPublicationRequestsQ
      .filter(_.id == lift(id))

  inline def updateQ(
    inline id: PublicationRequestId,
    inline status: DbPublicationRequestStatus,
    inline reason: Option[String],
  ) =
    ingredientPublicationRequestsQ
      .filter(_.id == lift(id))
      .update(
        _.status -> lift(status),
        _.reason -> lift(reason),
      )

object IngredientPublicationRequestsRepo:
  def layer: RLayer[DataSource, IngredientPublicationRequestsRepo] =
    ZLayer.fromFunction(IngredientPublicationRequestsRepoLive.apply)

package db.repositories

import db.DbError
import db.tables.DbIngredient
import db.tables.publication.{DbIngredientPublicationRequest, DbPublicationRequestStatus}
import db.tables.publication.DbPublicationRequestStatus.Pending
import domain.{IngredientId, PublicationRequestId, PublicationRequestNotFound}

import io.getquill.*
import javax.sql.DataSource
import java.util.UUID
import zio.{IO, RLayer, ZLayer, ZIO}

trait IngredientPublicationRequestsRepo:
  def requestPublication(ingredientId: IngredientId): IO[DbError, PublicationRequestId]
  def getAllPending: IO[DbError, Seq[DbIngredientPublicationRequest]]
  def get(id: PublicationRequestId): IO[DbError, Option[DbIngredientPublicationRequest]]
  def getWithIngredient(id: PublicationRequestId): IO[DbError, Option[(DbIngredientPublicationRequest, DbIngredient)]]
  def updateStatus(id: PublicationRequestId, status: PublicationRequestStatus): IO[DbError, Boolean]

final case class IngredientPublicationRequestsRepoLive(dataSource: DataSource)
  extends IngredientPublicationRequestsRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import IngredientPublicationRequestsQueries.*

  private given DataSource = dataSource

  override def requestPublication(ingredientId: IngredientId): IO[DbError, PublicationRequestId] =
    run(requestPublicationQ(lift(ingredientId))).provideDS

  override def getAllPendingIds: IO[DbError, Vector[PublicationRequestId]] = {
    run(allPendingQ.map(_.id)).provideDS.map(Vector.from)
  }

  override def get(id: PublicationRequestId): IO[DbError, Option[DbIngredientPublicationRequest]] =
    run(getQ(id)).map(_.headOption).provideDS

  override def updateStatus(id: PublicationRequestId, status: PublicationRequestStatus):
    IO[DbError, Boolean] =
    val (dbStatus, reason) = DbPublicationRequestStatus.fromDomain(status)
    run(updateQ(id, dbStatus, reason)).map(_ > 0).provideDS

  override def getWithIngredient(id: PublicationRequestId):
    IO[DbError, Option[(DbIngredientPublicationRequest, DbIngredient)]] =

    run(
      getQ(id)
        .join(IngredientsQueries.ingredientsQ)
        .on((rpq, r) => rpq.ingredientId == r.id)
    ).map(_.headOption).provideDS

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
      .take(1)

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

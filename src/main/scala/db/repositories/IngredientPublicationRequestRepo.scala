package db.repositories

import db.DbError
import db.tables.DbIngredient
import db.tables.publication.{DbIngredientPublicationRequest, DbPublicationRequestStatus}
import domain.{IngredientId, PublicationRequestId, PublicationRequestStatus}

import io.getquill.*
import javax.sql.DataSource
import java.util.UUID
import zio.{IO, RLayer, ZLayer, ZIO}

trait IngredientPublicationRequestsRepo:
  def requestPublication(ingredientId: IngredientId): IO[DbError, PublicationRequestId]
  def getPendingRequestsWithIngredients: IO[DbError, Seq[(DbIngredientPublicationRequest, DbIngredient)]]
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

  override def getPendingRequestsWithIngredients:
    IO[DbError, Seq[(DbIngredientPublicationRequest, DbIngredient)]] =
    run(
      pendingRequestsQ
        .join(IngredientsQueries.ingredientsQ)
        .on(_.ingredientId == _.id)
    ).provideDS

  override def get(id: PublicationRequestId): IO[DbError, Option[DbIngredientPublicationRequest]] =
    run(getQ(id)).map(_.headOption).provideDS

  override def updateStatus(id: PublicationRequestId, status: PublicationRequestStatus):
    IO[DbError, Boolean] =
    val (dbStatus, reason) = DbPublicationRequestStatus.fromDomain(status)
    run(updateQ(id, dbStatus, reason)).map(_ > 0).provideDS

object IngredientPublicationRequestsQueries:
  import db.QuillConfig.ctx.*

  inline def requestsQ = query[DbIngredientPublicationRequest]

  inline def requestPublicationQ(inline ingredientId: IngredientId):
    ActionReturning[DbIngredientPublicationRequest, UUID] =
    requestsQ
      .insert(_.ingredientId -> ingredientId)
      .returningGenerated(_.id)

  inline def pendingRequestsQ: EntityQuery[DbIngredientPublicationRequest] =
    requestsQ
      .filter(r => infix"${r.status} = 'pending'::publication_request_status".as[Boolean])

  inline def pendingRequestsByIngredientIdQ(inline ingredientId: IngredientId):
    EntityQuery[DbIngredientPublicationRequest] =
    pendingRequestsQ
      .filter(_.ingredientId == ingredientId)

  inline def getQ(inline id: PublicationRequestId): EntityQuery[DbIngredientPublicationRequest] =
    requestsQ
      .filter(_.id == lift(id))

  inline def updateQ(
    inline id: PublicationRequestId,
    inline status: DbPublicationRequestStatus,
    inline reason: Option[String],
  ): Update[DbIngredientPublicationRequest] =
    requestsQ
      .filter(_.id == lift(id))
      .update(
        _.status -> lift(status),
        _.reason -> lift(reason),
      )

object IngredientPublicationRequestsRepo:
  def layer: RLayer[DataSource, IngredientPublicationRequestsRepo] =
    ZLayer.fromFunction(IngredientPublicationRequestsRepoLive.apply)

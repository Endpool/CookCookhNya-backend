package db.repositories

import api.Authentication.AuthenticatedUser
import db.DbError
import db.tables.DbIngredient
import db.tables.publication.{DbIngredientPublicationRequest, DbPublicationRequestStatus}
import domain.{IngredientId, PublicationRequestId, PublicationRequestStatus, UserId}
import io.getquill.*

import javax.sql.DataSource
import java.util.UUID
import zio.{IO, RLayer, ZIO, ZLayer}

trait IngredientPublicationRequestsRepo:
  def requestPublication(ingredientId: IngredientId): IO[DbError, PublicationRequestId]
  def getPendingRequestsWithIngredients: IO[DbError, Seq[(DbIngredientPublicationRequest, DbIngredient)]]
  def get(id: PublicationRequestId): IO[DbError, Option[DbIngredientPublicationRequest]]
  def getWithIngredient(id: PublicationRequestId): IO[DbError, Option[(DbIngredientPublicationRequest, DbIngredient)]]
  def updateStatus(id: PublicationRequestId, status: PublicationRequestStatus): IO[DbError, Boolean]
  def getAllCreatedBy: ZIO[AuthenticatedUser, DbError, List[DbIngredientPublicationRequest]]

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
    run(getQ(lift(id))).map(_.headOption).provideDS

  override def updateStatus(id: PublicationRequestId, status: PublicationRequestStatus):
    IO[DbError, Boolean] =
    val (dbStatus, reason) = DbPublicationRequestStatus.fromDomain(status)
    run(updateQ(lift(id), lift(dbStatus), lift(reason))).map(_ > 0).provideDS

  override def getWithIngredient(id: PublicationRequestId):
    IO[DbError, Option[(DbIngredientPublicationRequest, DbIngredient)]] =

    run(
      requestsQ
        .join(IngredientsQueries.ingredientsQ)
        .on(_.ingredientId == _.id)
    ).map(_.headOption).provideDS

  override def getAllCreatedBy: ZIO[AuthenticatedUser, DbError, List[DbIngredientPublicationRequest]] =
    for
      userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
      res <- run(getAllCreatedByQ(lift(userId))).provideDS
    yield res
    
object IngredientPublicationRequestsQueries:
  inline def requestsQ: EntityQuery[DbIngredientPublicationRequest] =
    query[DbIngredientPublicationRequest]

  inline def requestPublicationQ(inline ingredientId: IngredientId):
    ActionReturning[DbIngredientPublicationRequest, UUID] =
    requestsQ
      .insert(_.ingredientId -> ingredientId)
      .returningGenerated(_.id)

  inline def pendingRequestsQ: EntityQuery[DbIngredientPublicationRequest] =
    requestsQ
      .filter(r =>
        infix"${r.status} = 'pending'::publication_request_status"
          .asCondition
      )

  inline def pendingRequestsByIngredientIdQ(inline ingredientId: IngredientId):
    EntityQuery[DbIngredientPublicationRequest] =
    pendingRequestsQ
      .filter(_.ingredientId == ingredientId)

  inline def getQ(inline id: PublicationRequestId): EntityQuery[DbIngredientPublicationRequest] =
    requestsQ
      .filter(_.id == id)

  inline def updateQ(
    inline id: PublicationRequestId,
    inline status: DbPublicationRequestStatus,
    inline reason: Option[String],
  ): Update[DbIngredientPublicationRequest] =
    requestsQ
      .filter(_.id == id)
      .update(
        _.status -> status,
        _.reason -> reason,
      )

  inline def getAllCreatedByQ(inline userId: UserId): Query[DbIngredientPublicationRequest] =
    query[DbIngredientPublicationRequest]
      .join(query[DbIngredientPublicationRequest])
      .on(_.ingredientId == _.id)
      .map(_._1)


object IngredientPublicationRequestsRepo:
  def layer: RLayer[DataSource, IngredientPublicationRequestsRepo] =
    ZLayer.fromFunction(IngredientPublicationRequestsRepoLive.apply)

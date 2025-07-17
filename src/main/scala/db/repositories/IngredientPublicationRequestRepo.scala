package db.repositories

import db.DbError
import db.tables.DbIngredient
import db.tables.publication.{DbIngredientPublicationRequest, DbPublicationRequestStatus}
import db.tables.publication.DbPublicationRequestStatus.Pending
import domain.{IngredientId, PublicationRequestId, PublicationRequestNotFound}
import io.getquill.*

import javax.sql.DataSource
import zio.{IO, RLayer, ZIO, ZLayer}

trait IngredientPublicationRequestsRepo:
  def requestPublication(ingredientId: IngredientId): IO[DbError, Unit]
  def getAllPendingIds: IO[DbError, Vector[PublicationRequestId]]
  def get(id: PublicationRequestId): IO[DbError, Option[DbIngredientPublicationRequest]]
  def getWithIngredient(id: PublicationRequestId): IO[DbError, Option[(DbIngredientPublicationRequest, DbIngredient)]]
  def update(id: PublicationRequestId, comment: String, status: DbPublicationRequestStatus):
    IO[DbError | PublicationRequestNotFound, Unit]

final case class IngredientPublicationRequestsRepoLive(dataSource: DataSource)
  extends IngredientPublicationRequestsRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import IngredientPublicationRequestsQueries.*

  private given DataSource = dataSource

  override def requestPublication(ingredientId: IngredientId): IO[DbError, Unit] =
    run(requestPublicationQ(lift(ingredientId))).unit.provideDS

  override def getAllPendingIds: IO[DbError, Vector[PublicationRequestId]] = {
    run(allPendingQ.map(_.id)).provideDS.map(Vector.from)
  }

  override def get(id: PublicationRequestId): IO[DbError, Option[DbIngredientPublicationRequest]] =
    run(getQ(id)).map(_.headOption).provideDS

  override def update(id: PublicationRequestId, comment: String, status: DbPublicationRequestStatus):
    IO[DbError | PublicationRequestNotFound, Unit] =
    run(updateQ(id, comment, status)).provideDS.flatMap {
      case 0 => ZIO.fail(PublicationRequestNotFound(id))
      case _ => ZIO.unit
    }

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

  inline def requestPublicationQ(inline ingredientId: IngredientId): Insert[DbIngredientPublicationRequest] =
    ingredientPublicationRequestsQ
      .insert(_.ingredientId -> ingredientId)

  inline def allPendingQ = 
    ingredientPublicationRequestsQ
      .filter(_.status == lift(Pending))

  inline def pendingRequestsByIdQ(inline ingredientId: IngredientId) = allPendingQ.filter(_.ingredientId == ingredientId)

  inline def getQ(inline id: PublicationRequestId) =
    ingredientPublicationRequestsQ
      .filter(_.id == lift(id))
      .take(1)

  inline def updateQ(inline id: PublicationRequestId, inline comment: String, inline status: DbPublicationRequestStatus) =
    ingredientPublicationRequestsQ
      .filter(_.id == lift(id))
      .update(
        _.comment -> lift(comment),
        _.status -> lift(status)
      )

object IngredientPublicationRequestsRepo:
  def layer: RLayer[DataSource, IngredientPublicationRequestsRepo] =
    ZLayer.fromFunction(IngredientPublicationRequestsRepoLive.apply)

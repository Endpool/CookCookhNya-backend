package db.repositories

import db.DbError
import db.tables.publication.DbIngredientPublicationRequest
import db.tables.publication.DbPublicationRequestStatus.Pending
import domain.IngredientId
import io.getquill.*
import zio.{IO, RLayer, ZLayer}

import java.util.UUID
import javax.sql.DataSource

trait IngredientPublicationRequestsRepo:
  def requestPublication(ingredientId: IngredientId): IO[DbError, Unit]
  def getAllPending: IO[DbError, Seq[DbIngredientPublicationRequest]]
  def get(id: UUID): IO[DbError, Option[DbIngredientPublicationRequest]]

private inline def ingredientPublicationRequests = query[DbIngredientPublicationRequest]

final case class IngredientPublicationRequestsRepoLive(dataSource: DataSource)
  extends IngredientPublicationRequestsRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import IngredientPublicationRequestsQueries.*

  private given DataSource = dataSource

  override def requestPublication(ingredientId: IngredientId): IO[DbError, Unit] =
    run(requestPublicationQ(lift(ingredientId))).unit.provideDS

  override def getAllPending: IO[DbError, Seq[DbIngredientPublicationRequest]] =
    run(allPendingQ).provideDS

  override def get(id: UUID): IO[DbError, Option[DbIngredientPublicationRequest]] =
    run(getQ(id)).map(_.headOption).provideDS

object IngredientPublicationRequestsQueries:
  import db.QuillConfig.ctx.*
  inline def requestPublicationQ(inline ingredientId: IngredientId): Insert[DbIngredientPublicationRequest] =
    ingredientPublicationRequests.insert(_.ingredientId -> ingredientId)
    
  inline def allPendingQ = ingredientPublicationRequests.filter(_.status == lift(Pending))
  inline def pendingRequestsByIdQ(inline ingredientId: IngredientId) = allPendingQ.filter(_.ingredientId == ingredientId)

  inline def getQ(inline id: UUID) =
    ingredientPublicationRequests.filter(_.id == lift(id)).take(1)

object IngredientPublicationRequestsRepo:
  def layer: RLayer[DataSource, IngredientPublicationRequestsRepo] =
    ZLayer.fromFunction(IngredientPublicationRequestsRepoLive.apply)
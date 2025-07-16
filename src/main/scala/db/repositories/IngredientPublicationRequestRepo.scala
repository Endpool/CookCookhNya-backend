package db.repositories

import db.DbError
import db.tables.publication.DbIngredientPublicationRequest
import db.tables.publication.DbPublicationRequestStatus.Pending
import domain.IngredientId
import io.getquill.*
import zio.{IO, RLayer, ZLayer}

import javax.sql.DataSource

trait IngredientPublicationRequestsRepo:
  def requestPublication(ingredientId: IngredientId): IO[DbError, Unit]

private inline def ingredientPublicationRequests = query[DbIngredientPublicationRequest]

final case class IngredientPublicationRequestsRepoLive(dataSource: DataSource)
  extends IngredientPublicationRequestsRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import IngredientPublicationRequestsQueries.*

  private given DataSource = dataSource

  override def requestPublication(ingredientId: IngredientId): IO[DbError, Unit] =
    run(requestPublicationQ(lift(ingredientId))).unit.provideDS

object IngredientPublicationRequestsQueries:
  import db.QuillConfig.ctx.*
  inline def requestPublicationQ(inline ingredientId: IngredientId): Insert[DbIngredientPublicationRequest] =
    ingredientPublicationRequests.insert(_.ingredientId -> ingredientId)
    
  inline def allPendingQ = ingredientPublicationRequests.filter(_.status == lift(Pending))
  inline def pendningRequestByIdQ(inline ingredientId: IngredientId) = allPendingQ.filter(_.ingredientId == ingredientId) 

object IngredientPublicationRequestsRepo:
  def layer: RLayer[DataSource, IngredientPublicationRequestsRepo] =
    ZLayer.fromFunction(IngredientPublicationRequestsRepoLive.apply)
package db.repositories

import db.DbError
import db.tables.publication.DbIngredientPublicationRequest
import domain.IngredientId

import io.getquill.*
import zio.{IO, ZLayer, RLayer}
import javax.sql.DataSource

trait IngredientPublicationRequestsRepo:
  def requestPublication(ingredientId: IngredientId): IO[DbError, Unit]

private inline def ingredientPublicationRequests = query[DbIngredientPublicationRequest]

final case class IngredientPublicationRequestsRepoLive(dataSource: DataSource)
  extends IngredientPublicationRequestsRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import RecipePublicationRequestsQueries.*

  private given DataSource = dataSource

  override def requestPublication(ingredientId: IngredientId): IO[DbError, Unit] =
    run(requestPublicationQ(lift(ingredientId))).unit.provideDS

object IngredientPublicationRequestsQueries:
  inline def requestPublicationQ(inline ingredientId: IngredientId) =
    ingredientPublicationRequests.insert(_.ingredientId -> ingredientId)

object IngredientPublicationRequestsRepo:
  def layer: RLayer[DataSource, RecipePublicationRequestsRepo] =
    ZLayer.fromFunction(RecipePublicationRequestsRepoLive.apply)
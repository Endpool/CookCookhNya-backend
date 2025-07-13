package db.repositories

import db.DbError
import db.tables.DbRecipePublicationRequest
import domain.RecipeId

import io.getquill.*
import zio.{IO, ZLayer, RLayer}
import javax.sql.DataSource

trait RecipePublicationRequestsRepo:
  def publish(recipeId: RecipeId): IO[DbError, Unit]

private inline def recipePublicationRequests = query[DbRecipePublicationRequest]

final case class RecipePublicationRequestsRepoLive(dataSource: DataSource)
  extends RecipePublicationRequestsRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import RecipePublicationRequestsQueries.*

  private given DataSource = dataSource

  override def publish(recipeId: RecipeId): IO[DbError, Unit] =
    run(publishQ(lift(recipeId))).unit.provideDS

object RecipePublicationRequestsQueries:
  inline def publishQ(inline recipeId: RecipeId) =
    recipePublicationRequests.insert(_.recipeId -> recipeId)

object RecipePublicationRequestsRepo:
  def layer: RLayer[DataSource, RecipePublicationRequestsRepo] =
    ZLayer.fromFunction(RecipePublicationRequestsRepoLive.apply)

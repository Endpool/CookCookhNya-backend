package db.repositories

import db.DbError
import db.tables.DbRecipePublicationRequest
import domain.RecipeId

import io.getquill.*
import zio.{IO, ZLayer, RLayer}
import javax.sql.DataSource

trait RecipePublicationRequestsRepo:
  def requestPublication(recipeId: RecipeId): IO[DbError, Unit]

private inline def recipePublicationRequests = query[DbRecipePublicationRequest]

final case class RecipePublicationRequestsRepoLive(dataSource: DataSource)
  extends RecipePublicationRequestsRepo:
  import db.QuillConfig.ctx.*
  import db.QuillConfig.provideDS
  import RecipePublicationRequestsQueries.*

  private given DataSource = dataSource

  override def requestPublication(recipeId: RecipeId): IO[DbError, Unit] =
    run(requestPublicationQ(lift(recipeId))).unit.provideDS

object RecipePublicationRequestsQueries:
  inline def requestPublicationQ(inline recipeId: RecipeId) =
    recipePublicationRequests.insert(_.recipeId -> recipeId)

object RecipePublicationRequestsRepo:
  def layer: RLayer[DataSource, RecipePublicationRequestsRepo] =
    ZLayer.fromFunction(RecipePublicationRequestsRepoLive.apply)

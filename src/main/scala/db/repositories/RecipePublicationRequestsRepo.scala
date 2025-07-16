package db.repositories

import db.DbError
import db.tables.publication.DbPublicationRequestStatus.Pending
import db.tables.publication.DbRecipePublicationRequest
import domain.RecipeId
import io.getquill.*
import zio.{IO, RLayer, ZLayer}

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
  import db.QuillConfig.ctx.*
  
  inline def requestPublicationQ(inline recipeId: RecipeId) =
    recipePublicationRequests.insert(_.recipeId -> recipeId)

  inline def allPendingQ = ingredientPublicationRequests.filter(_.status == lift(Pending))
  inline def pendningRequestsByIdQ(inline recipeId: RecipeId) = allPendingQ.filter(_.ingredientId == recipeId)
  
object RecipePublicationRequestsRepo:
  def layer: RLayer[DataSource, RecipePublicationRequestsRepo] =
    ZLayer.fromFunction(RecipePublicationRequestsRepoLive.apply)

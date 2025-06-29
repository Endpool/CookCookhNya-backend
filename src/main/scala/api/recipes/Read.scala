package api.recipes

import api.AppEnv
import api.variantJson
import com.augustnagro.magnum.magzio
import db.repositories.{catchAllAsDbError, RecipeIngredientsRepo, RecipesDomainRepo, RecipesRepo}
import domain.{DbError, IngredientId, RecipeError, RecipeId, StorageId}
import db.tables.{DbStorage, DbStorageCreator, ingredientsTable, recipeIngredientsTable, recipesTable, storageIngredientsTable, storageMembersTable, storagesTable}

import io.circe.generic.auto.*
import sttp.model.StatusCode.{InternalServerError, NotFound}
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import com.augustnagro.magnum.magzio.*
import zio.ZIO


private case class IngredientSummary(
                                    id: IngredientId,
                                    name: String,
                                    inStorages: Vector[StorageId]
                                    )
private case class RecipeResp(
                             ingredients: Vector[IngredientSummary],
                             name: String,
                             sourceLing: String
                             )

val get: ZServerEndpoint[AppEnv, Any] =
  recipesEndpoint
    .get
    .in(path[RecipeId])
    .errorOut(oneOf(
      NotFound.variantJson[RecipeError.NotFound],
      InternalServerError.variantJson[DbError.UnexpectedDbError])
    )
    .out(jsonBody[RecipeResp])
    .zServerLogic(getHandler)

def getHandler(recipeId: RecipeId): ZIO[Transactor, DbError.UnexpectedDbError | RecipeError.NotFound, RecipeResp] =
  val dbEffect = ZIO.serviceWithZIO[magzio.Transactor](_.transact {
    val frag =
      sql"""
        SELECT r.${recipesTable.name} AS "name", r.${recipesTable.sourceLink} AS sourceLink,
        (
          SELECT JSON_AGG(
            JSON_BUILD_OBJECT(
              'id', i.${ingredientsTable.id},
              'name', i.${ingredientsTable.name},
              'inStorages', (
                SELECT JSON_AGG(DISTINCT si.storage_id)
                FROM $storageIngredientsTable si
                WHERE si.${storageIngredientsTable.ingredientId} = i.${ingredientsTable.id}
              )
            )
          )
          FROM $ingredientsTable i
          JOIN $recipeIngredientsTable ri ON i.${ingredientsTable.id}= ri.${recipeIngredientsTable.recipeId}
          WHERE ri.${recipeIngredientsTable.recipeId} = r.id
        ) AS "ingredients"
        FROM $recipesTable r
        WHERE r.${recipesTable.id} = $recipeId
        LIMIT 1;
        """
    frag.query[RecipeResp].run().headOption
  })

  dbEffect.foldZIO(
    error => ZIO.fail(DbError.UnexpectedDbError(error.getMessage)),
    {
      case Some(recipe) => ZIO.succeed(recipe)
      case None => ZIO.fail(RecipeError.NotFound(recipeId))
    }
  )
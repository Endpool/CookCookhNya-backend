package api.recipes

import api.AppEnv
import api.variantJson
import com.augustnagro.magnum.magzio
import db.repositories.{catchAllAsDbError, RecipeIngredientsRepo, RecipesDomainRepo, RecipesRepo}
import domain.{DbError, IngredientId, RecipeError, RecipeId, StorageId}
import db.tables.{DbStorage, DbStorageCreator, ingredientsTable, recipeIngredientsTable, recipesTable, storageIngredientsTable, storageMembersTable, storagesTable}

import io.circe.generic.auto.*
import io.circe.parser.decode
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
                             sourceLink: String
                             )

// intermediate class to accept raw query result
private case class RawRecipeResult(
                                    name: String,
                                    sourceLink: String,
                                    ingredients: String // JSON string from PostgreSQL
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
        COALESCE(
          (
            SELECT JSON_AGG(
              JSON_BUILD_OBJECT(
                'id', i.${ingredientsTable.id},
                'name', i.${ingredientsTable.name},
                'inStorages', COALESCE(
                  (
                    SELECT JSON_AGG(DISTINCT si.storage_id)
                    FROM $storageIngredientsTable si
                    WHERE si.${storageIngredientsTable.ingredientId} = i.${ingredientsTable.id}
                  ),
                  '[]'::json
                )
              )
            )
            FROM $ingredientsTable i
            JOIN $recipeIngredientsTable ri ON i.${ingredientsTable.id}= ri.${recipeIngredientsTable.ingredientId}
            WHERE ri.${recipeIngredientsTable.recipeId} = r.${recipesTable.id}
          ),
          '[]'::json
        ) AS ingredients
        FROM $recipesTable r
        WHERE r.${recipesTable.id} = $recipeId;
        """
    frag.query[RawRecipeResult].run().headOption
  })

  dbEffect.foldZIO(
    error => ZIO.fail(DbError.UnexpectedDbError(error.getMessage)),
    {
      case Some(rawResult) =>
        // Parse the JSON ingredients string
        decode[Vector[IngredientSummary]](rawResult.ingredients) match
          case Right(ingredients) =>
            ZIO.succeed(RecipeResp(
              ingredients, rawResult.name, rawResult.sourceLink
            ))
          case Left(_) =>
            ZIO.fail(DbError.UnexpectedDbError(s"Failed to parse ingredients JSON: ${rawResult.ingredients}"))

      case None => ZIO.fail(RecipeError.NotFound(recipeId))
    }
  )
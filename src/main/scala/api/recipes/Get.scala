package api.recipes

import api.{AppEnv, handleFailedSqlQuery, zSecuredServerLogic}
import api.EndpointErrorVariants.{recipeNotFoundVariant, storageNotFoundVariant}
import domain.{IngredientId, InternalServerError, RecipeId, StorageId, UserId}
import domain.RecipeError.NotFound
import db.tables.{ingredientsTable, recipeIngredientsTable, recipesTable, storageIngredientsTable, storageMembersTable, storagesTable}
import db.{DbError, handleDbError}

import com.augustnagro.magnum.magzio
import com.augustnagro.magnum.magzio.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
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

val get: ZServerEndpoint[AppEnv, Any] =
  recipesEndpoint
    .securityIn(auth.bearer[UserId]())
    .get
    .in(path[RecipeId])
    .errorOut(oneOf(recipeNotFoundVariant, storageNotFoundVariant))
    .out(jsonBody[RecipeResp])
    .zSecuredServerLogic(getHandler)

// intermediate class to accept raw query result
private case class RawRecipeResult(
  name: String,
  sourceLink: String,
  ingredients: String // JSON string from PostgreSQL
)

def getHandler(userId: UserId)(recipeId: RecipeId):
  ZIO[Transactor, InternalServerError | NotFound, RecipeResp] =
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
                      AND si.${storageIngredientsTable.storageId} IN (
                        SELECT ${storageMembersTable.storageId} FROM $storageMembersTable
                        WHERE ${storageMembersTable.memberId} = $userId

                        UNION

                        SELECT ${storagesTable.id}
                        FROM $storagesTable
                        WHERE ${storagesTable.ownerId} = $userId
                      )
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

  dbEffect.mapError(handleDbError).flatMap {
    case Some(rawResult) =>
      // Parse the JSON ingredients string
      decode[Vector[IngredientSummary]](rawResult.ingredients) match
        case Right(ingredients) =>
          ZIO.succeed(RecipeResp(
            ingredients, rawResult.name, rawResult.sourceLink
          ))
        case Left(_) =>
          ZIO.fail(InternalServerError(s"Failed to parse ingredients JSON: ${rawResult.ingredients}"))
    case None => ZIO.fail(NotFound(recipeId.toString))
  }.mapError {
    case _: DbError => InternalServerError()
    case e: (InternalServerError | NotFound) => e
  }

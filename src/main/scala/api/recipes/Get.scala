package api.recipes

import api.{handleFailedSqlQuery, toStorageNotFound, toUserNotFound}
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{recipeNotFoundVariant, storageNotFoundVariant, serverErrorVariant}
import db.{DbError, handleDbError}
import db.tables.{ingredientsTable, recipeIngredientsTable, recipesTable, storageIngredientsTable, storageMembersTable, storagesTable}
import domain.{
  IngredientId,
  InternalServerError,
  RecipeId,
  RecipeNotFound,
  StorageId,
  UserId,
  UserNotFound,
}

import com.augustnagro.magnum.magzio
import com.augustnagro.magnum.magzio.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

case class IngredientSummary(
  id: IngredientId,
  name: String,
  inStorages: Vector[StorageId]
)

case class RecipeResp(
  ingredients: Vector[IngredientSummary],
  name: String,
  sourceLink: String
)

private type GetEnv = Transactor

private val get: ZServerEndpoint[GetEnv, Any] =
  recipesEndpoint
    .get
    .in(path[RecipeId]("recipeId"))
    .errorOut(oneOf(recipeNotFoundVariant, storageNotFoundVariant, serverErrorVariant))
    .out(jsonBody[RecipeResp])
    .zSecuredServerLogic(getHandler)

// intermediate class to accept raw query result
private case class RawRecipeResult(
  name: String,
  sourceLink: String,
  ingredients: String // JSON string from PostgreSQL
)

private def getHandler(recipeId: RecipeId):
  ZIO[AuthenticatedUser & GetEnv,
      InternalServerError | RecipeNotFound | UserNotFound,
      RecipeResp] =
  ZIO.serviceWithZIO[AuthenticatedUser] { authenticatedUser =>
    val userId = authenticatedUser.userId
    ZIO.serviceWithZIO[Transactor](_.transact(
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
      """.query[RawRecipeResult].run().headOption
    ))
  }.mapError(handleDbError).flatMap {
    case Some(rawResult) =>
      // Parse the JSON ingredients string
      decode[Vector[IngredientSummary]](rawResult.ingredients) match
        case Right(ingredients) =>
          ZIO.succeed(RecipeResp(
            ingredients, rawResult.name, rawResult.sourceLink
          ))
        case Left(_) =>
          ZIO.fail(InternalServerError(s"Failed to parse ingredients JSON: ${rawResult.ingredients}"))
    case None => ZIO.fail(RecipeNotFound(recipeId.toString))
  }.mapError {
    case e: DbError.FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(toUserNotFound)
      .getOrElse(InternalServerError())
    case _: DbError => InternalServerError()
    case e: (InternalServerError | RecipeNotFound | UserNotFound) => e
  }

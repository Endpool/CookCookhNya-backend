package api.recipes

import api.{handleFailedSqlQuery, toUserNotFound}
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{recipeNotFoundVariant, serverErrorVariant, userNotFoundVariant}
import db.{DbError, handleDbError}
import db.tables.{usersTable, ingredientsTable, recipeIngredientsTable, recipesTable, storageIngredientsTable, storageMembersTable, storagesTable}
import domain.{
  IngredientId,
  InternalServerError,
  RecipeId,
  RecipeNotFound,
  StorageId,
  UserId,
  UserNotFound,
}

import com.augustnagro.magnum.magzio.*
import com.augustnagro.magnum.Query
import io.circe.generic.auto.*
import io.circe.parser.decode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

final case class IngredientResp(
  id: IngredientId,
  name: String,
  inStorages: Vector[StorageSummary],
)

final case class StorageSummary(
  id: StorageId,
  name: String
)

final case class RecipeCreatorResp(
  id: UserId,
  fullName: String
)

final case class RecipeResp(
  ingredients: Vector[IngredientResp],
  name: String,
  sourceLink: Option[String],
  creator: Option[RecipeCreatorResp],
)

private type GetEnv = Transactor

private val get: ZServerEndpoint[GetEnv, Any] =
  recipesEndpoint
    .get
    .in(path[RecipeId]("recipeId"))
    .errorOut(oneOf(serverErrorVariant, recipeNotFoundVariant, userNotFoundVariant))
    .out(jsonBody[RecipeResp])
    .zSecuredServerLogic(getHandler)

// intermediate class to accept raw query result
private case class RawRecipeResult(
  name: String,
  sourceLink: Option[String],
  creatorId: Option[UserId],
  creatorFullName: Option[String],
  ingredients: String, // JSON string from PostgreSQL
)

private def getHandler(recipeId: RecipeId):
  ZIO[AuthenticatedUser & GetEnv,
      InternalServerError | RecipeNotFound | UserNotFound,
      RecipeResp] =
  ZIO.serviceWithZIO[AuthenticatedUser] { authenticatedUser =>
    val userId = authenticatedUser.userId
    ZIO.serviceWithZIO[Transactor](_
      .transact(rawRecipeQuery(userId, recipeId).run().headOption)
      .mapError(handleDbError)
    )
  }.someOrFail(RecipeNotFound(recipeId)).flatMap { rawResult =>
    // Parse the JSON ingredients string
    ZIO.fromEither(decode[Vector[IngredientResp]](rawResult.ingredients))
      .map {
        (rawResult.creatorId, rawResult.creatorFullName) match
          case (Some(creatorId), Some(creatorFullName)) => RecipeResp(
            _,
            rawResult.name,
            rawResult.sourceLink,
            Some(RecipeCreatorResp(
              creatorId,
              creatorFullName,
            )),
          )
          case _ => RecipeResp(
            _,
            rawResult.name,
            rawResult.sourceLink,
            None
          )
      }
      .orElseFail(InternalServerError(s"Failed to parse ingredients JSON: ${rawResult.ingredients}"))
  }.mapError {
    case e: DbError.FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(toUserNotFound)
      .getOrElse(InternalServerError())
    case _: DbError => InternalServerError()
    case e: (InternalServerError | RecipeNotFound | UserNotFound) => e
  }

private inline def rawRecipeQuery(
  inline userId: UserId,
  inline recipeId: RecipeId
): Query[RawRecipeResult] = sql"""
  SELECT
    r.${recipesTable.name} AS "name",
    r.${recipesTable.sourceLink} AS "sourceLink",
    u.${usersTable.id} as "creatorId",
    u.${usersTable.fullName} as "creatorFullName",
    COALESCE(
      (
        SELECT
          JSON_AGG(JSON_BUILD_OBJECT(
            'id', i.${ingredientsTable.id},
            'name', i.${ingredientsTable.name},
            'inStorages', COALESCE(
              (
                SELECT JSON_AGG(JSON_BUILD_OBJECT(
                  'id', si.${storageIngredientsTable.storageId},
                  'name', s.${storagesTable.name}
                ))
                FROM $storageIngredientsTable si
                JOIN $storagesTable AS s ON si.${storageIngredientsTable.storageId} = s.${storagesTable.id}
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
          ))
        FROM $ingredientsTable i
        JOIN $recipeIngredientsTable ri
          ON i.${ingredientsTable.id} = ri.${recipeIngredientsTable.ingredientId}
        WHERE ri.${recipeIngredientsTable.recipeId} = r.${recipesTable.id}
      ),
      '[]'::json
    ) AS "ingredients"
  FROM $recipesTable r
  RIGHT JOIN $usersTable u ON r.${recipesTable.creatorId} = u.${usersTable.id}
  WHERE r.${recipesTable.id} = $recipeId
    AND (r.${recipesTable.isPublished} = true OR r.${recipesTable.creatorId} = $userId);
""".query[RawRecipeResult]

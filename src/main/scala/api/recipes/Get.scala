package api.recipes

import api.{handleFailedSqlQuery, toUserNotFound}
import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{recipeNotFoundVariant, serverErrorVariant, userNotFoundVariant}
import api.moderation.pubrequests.PublicationRequestStatusResp
import db.{DbError, handleDbError}
import db.tables.{ingredientsTable, recipeIngredientsTable, recipesTable, storageIngredientsTable, storageMembersTable, storagesTable, usersTable}
import domain.{IngredientId, InternalServerError, RecipeId, RecipeNotFound, StorageId, UserId, UserNotFound}
import com.augustnagro.magnum.magzio.*
import com.augustnagro.magnum.Query
import db.repositories.RecipePublicationRequestsRepo
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
  moderationStatus: Option[PublicationRequestStatusResp]
)

private type GetEnv = Transactor & RecipePublicationRequestsRepo

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
      RecipeResp] = {

  def getLastPublicationRequestStatus =
    ZIO.serviceWithZIO[RecipePublicationRequestsRepo](_.getAllRequestsForRecipe(recipeId))
      .map(
        _.sortBy(_.updatedAt).lastOption.map(
          req => PublicationRequestStatusResp.fromDomain(req.status.toDomain(req.reason))
        )
      )

  {
    for
      rawResult <- ZIO.serviceWithZIO[AuthenticatedUser] { authenticatedUser =>
        val userId = authenticatedUser.userId
        ZIO.serviceWithZIO[Transactor](_
          .transact(rawRecipeQuery(userId, recipeId).run().headOption)
          .mapError(handleDbError)
        )
      }.someOrFail(RecipeNotFound(recipeId))
      status <- getLastPublicationRequestStatus
      result <- ZIO.fromEither(decode[Vector[IngredientResp]](rawResult.ingredients))
        // Parse the JSON ingredients string
        .map { ingredients =>
          val recipeCreatorResp = for
            creatorId <- rawResult.creatorId
            creatorFullName <- rawResult.creatorFullName
          yield RecipeCreatorResp(creatorId, creatorFullName)
          RecipeResp(
            ingredients,
            rawResult.name,
            rawResult.sourceLink,
            recipeCreatorResp,
            status
          )
        }.orElseFail(InternalServerError(s"Failed to parse ingredients JSON: ${rawResult.ingredients}"))
    yield result
  }.mapError {
    case e: DbError.FailedDbQuery => handleFailedSqlQuery(e)
      .flatMap(toUserNotFound)
      .getOrElse(InternalServerError())
    case _: DbError => InternalServerError()
    case e: (InternalServerError | RecipeNotFound | UserNotFound) => e
  }
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
  LEFT JOIN $usersTable u ON r.${recipesTable.creatorId} = u.${usersTable.id}
  WHERE r.${recipesTable.id} = $recipeId
    AND (r.${recipesTable.isPublished} OR r.${recipesTable.creatorId} = $userId);
""".query[RawRecipeResult]

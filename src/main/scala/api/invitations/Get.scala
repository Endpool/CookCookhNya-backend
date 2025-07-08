package api.invitations

import api.handleFailedSqlQuery
import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
import api.EndpointErrorVariants.{recipeNotFoundVariant, storageNotFoundVariant}
import db.{DbError, handleDbError}
import db.tables.{ingredientsTable, recipeIngredientsTable, recipesTable, storageIngredientsTable, storageMembersTable, storagesTable}
import domain.{IngredientId, InternalServerError, RecipeId, StorageId, UserId}
import domain.RecipeError.NotFound

import com.augustnagro.magnum.magzio
import com.augustnagro.magnum.magzio.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

package api

import domain.{
  IngredientNotFound,
  InternalServerError,
  InvalidInvitationHash,
  RecipeAccessForbidden,
  RecipeNotFound,
  StorageAccessForbidden,
  StorageNotFound,
  UserNotFound,
  PublicationRequestNotFound
}

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto.*
import scala.reflect.ClassTag
import sttp.model.StatusCode
import sttp.model.StatusCode.*
import sttp.tapir.{EndpointOutput, Schema}
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*

extension (sc: StatusCode)
  def variantJson[E : Encoder : Decoder : Schema : ClassTag]: EndpointOutput.OneOfVariant[E] =
    oneOfVariant(statusCode(sc).and(jsonBody[E]))

object EndpointErrorVariants:
  val ingredientNotFoundVariant = NotFound.variantJson[IngredientNotFound]
  val storageNotFoundVariant = NotFound.variantJson[StorageNotFound]
  val publicationRequestNotFound = NotFound.variantJson[PublicationRequestNotFound]
  val storageAccessForbiddenVariant = Forbidden.variantJson[StorageAccessForbidden]
  val recipeAccessForbiddenVariant = NotFound.variantJson[RecipeAccessForbidden]
  val userNotFoundVariant = NotFound.variantJson[UserNotFound]
  val recipeNotFoundVariant = NotFound.variantJson[RecipeNotFound]
  val serverErrorVariant = StatusCode.InternalServerError.variantJson[InternalServerError]
  val invalidInvitationHashVariant = BadRequest.variantJson[InvalidInvitationHash]

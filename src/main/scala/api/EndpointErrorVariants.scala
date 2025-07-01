package api

import domain.{
  ErrorResponse,
  IngredientError,
  StorageError,
  UserError,
  RecipeError,
  InternalServerError,
}

import io.circe.generic.auto.*
import io.circe.{Decoder, Encoder}
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import scala.reflect.ClassTag
import sttp.model.StatusCode.*
import sttp.tapir.EndpointOutput

extension (sc: StatusCode)
  def variantJson[E <: ErrorResponse : ClassTag]: EndpointOutput.OneOfVariant[E] =
    oneOfVariant(statusCode(sc).and(jsonBody[E]))

object EndpointErrorVariants:
  val ingredientNotFoundVariant  = NotFound.variantJson[IngredientError.NotFound]
  val storageNotFoundVariant = NotFound.variantJson[StorageError.NotFound]
  val userNotFoundVariant = NotFound.variantJson[UserError.NotFound]
  val recipeNotFoundVariant = NotFound.variantJson[RecipeError.NotFound]
  val serverErrorVariant = StatusCode.InternalServerError.variantJson[InternalServerError]
package api.endpoints

import api.domain.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.EndpointOutput
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*

trait IngredientEndpointsErrorOutput:
  protected val ingredientNotFoundVariant: EndpointOutput.OneOfVariant[IngredientError.NotFound] =
    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[IngredientError.NotFound]))
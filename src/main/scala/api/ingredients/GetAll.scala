package api.ingredients

import api.AppEnv
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.IngredientsRepo
import domain.InternalServerError

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

val getAll: ZServerEndpoint[AppEnv, Any] =
  ingredientsEndpoint
  .get
  .out(jsonBody[Seq[IngredientResp]])
  .out(statusCode(StatusCode.Ok))
  .errorOut(oneOf(serverErrorVariant))
  .zServerLogic(_ => getAllHandler)

val getAllHandler: ZIO[IngredientsRepo, InternalServerError, Seq[IngredientResp]] =
  ZIO.serviceWithZIO[IngredientsRepo](_.getAll).map(_.map(IngredientResp.fromDb))
    .mapError(_ => InternalServerError())

package api.ingredients

import api.AppEnv
import api.EndpointErrorVariants.serverErrorVariant
import api.zSecuredServerLogic
import db.repositories.IngredientsRepo
import domain.{InternalServerError, UserId}
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

val getAll: ZServerEndpoint[AppEnv, Any] =
  ingredientsEndpoint
  .securityIn(auth.bearer[UserId]())
  .get
  .out(jsonBody[Vector[IngredientResp]])
  .out(statusCode(StatusCode.Ok))
  .errorOut(oneOf(serverErrorVariant))
  .zSecuredServerLogic(u => _ => getAllHandler(u))

def getAllHandler(userId: UserId):
ZIO[AppEnv, InternalServerError, Vector[IngredientResp]] =
  ZIO.serviceWithZIO[IngredientsRepo](_.getAll(userId)).map(_.map(IngredientResp.fromDb))
    .mapError(_ => InternalServerError())

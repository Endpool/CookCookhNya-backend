package api.ingredients

import api.AppEnv
import db.repositories.IngredientsRepo

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{ZIO, URIO}

val getAll: ZServerEndpoint[AppEnv, Any] =
  ingredientsEndpoint
  .get
  .out(jsonBody[Seq[IngredientResp]])
  .out(statusCode(StatusCode.Ok))
  .zServerLogic(_ => getAllHandler)

val getAllHandler: URIO[IngredientsRepo, Seq[IngredientResp]] =
  ZIO.serviceWithZIO[IngredientsRepo](_.getAll).map(_.map(IngredientResp.fromDb))
    .catchAll { e => ??? } // TODO handle errors

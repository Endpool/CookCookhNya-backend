package api.ingredients

import api.AppEnv
import db.repositories.IngredientsRepo
import domain.Ingredient

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{ZIO, URIO}

val getAll: ZServerEndpoint[AppEnv, Any] =
  ingredientsEndpoint
  .get
  .out(jsonBody[Seq[Ingredient]])
  .out(statusCode(StatusCode.Ok))
  .zServerLogic(_ => getAllHandler)

val getAllHandler: URIO[IngredientsRepo, Seq[Ingredient]] =
  ZIO.serviceWithZIO[IngredientsRepo](_.getAll).map(_.map(_.toDomain))
    .catchAll { e => ??? } // TODO handle errors

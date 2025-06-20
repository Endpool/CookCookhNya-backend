package api.ingredients

import api.AppEnv
import db.repositories.IIngredientsRepo
import domain.Ingredient

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{ZIO, URIO}

val getAllIngredientsEndpoint: ZServerEndpoint[AppEnv, Any] = endpoint
  .get
  .in("ingredients")
  .out(statusCode(StatusCode.Ok))
  .out(jsonBody[Seq[Ingredient]])
  .zServerLogic(_ => getAllIngredients)

val getAllIngredients: URIO[IIngredientsRepo, Seq[Ingredient]] =
  ZIO.serviceWithZIO[IIngredientsRepo](_.getAll)

package api.endpoints.ingredient

import api.db.repositories.IIngredientRepo
import api.domain.Ingredient
import api.AppEnv

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

def getAllIngredients: URIO[IIngredientRepo, Seq[Ingredient]] =
  ZIO.serviceWithZIO[IIngredientRepo](_.getAll)

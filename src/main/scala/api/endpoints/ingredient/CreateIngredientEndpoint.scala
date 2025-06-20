package api.endpoints.ingredient

import api.AppEnv
import api.db.repositories.IIngredientsRepo
import api.domain.Ingredient

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

case class CreateIngredientReqBody(name: String)

val createIngredientEndpoint: ZServerEndpoint[AppEnv, Any] = endpoint
    .post
    .in("ingredients")
    .in(jsonBody[CreateIngredientReqBody])
    .out(jsonBody[Ingredient])
    .zServerLogic(createIngredient)

def createIngredient(reqBody: CreateIngredientReqBody): URIO[IIngredientsRepo, Ingredient] =
  ZIO.serviceWithZIO[IIngredientsRepo] {
    _.add(reqBody.name)
  }

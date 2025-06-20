package api.ingredients

import api.AppEnv
import db.repositories.IIngredientsRepo
import domain.Ingredient

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

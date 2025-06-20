package api.endpoints.ingredient

import api.db.repositories.IngredientRepoInterface
import api.domain.Ingredient
import api.AppEnv

import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

case class CreateIngredientReqBody(name: String)

val createIngredientEndpoint = endpoint
    .post
    .in("ingredients")
    .in(jsonBody[CreateIngredientReqBody])
    .out(jsonBody[Ingredient])
    .zServerLogic[IngredientRepoInterface](createIngredient)

def createIngredient(reqBody: CreateIngredientReqBody): URIO[IngredientRepoInterface, Ingredient] =
  ZIO.serviceWithZIO[IngredientRepoInterface] {
    _.add(Ingredient.CreationEntity(reqBody.name))
  }

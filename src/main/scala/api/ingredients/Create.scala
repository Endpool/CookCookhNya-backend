package api.ingredients

import api.AppEnv
import db.repositories.IngredientsRepo
import domain.Ingredient

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}

case class CreateIngredientReqBody(name: String)

private val create: ZServerEndpoint[AppEnv, Any] =
  ingredientsEndpoint
  .post
  .in(jsonBody[CreateIngredientReqBody])
  .out(jsonBody[Ingredient])
  .out(statusCode(StatusCode.Created))
  .zServerLogic(createHandler)

private def createHandler(reqBody: CreateIngredientReqBody): URIO[IngredientsRepo, Ingredient] =
  ZIO.serviceWithZIO[IngredientsRepo] {
    _.add(reqBody.name)
  }

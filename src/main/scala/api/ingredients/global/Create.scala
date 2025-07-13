package api.ingredients.global

import api.ingredients.CreateIngredientReqBody
import api.EndpointErrorVariants.serverErrorVariant
import db.repositories.IngredientsRepo
import domain.{IngredientId, InternalServerError}

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type CreateEnv = IngredientsRepo

private val createGlobal: ZServerEndpoint[CreateEnv, Any] =
  globalIngredientsEndpoint
  .post
  .in(jsonBody[CreateIngredientReqBody])
  .out(plainBody[IngredientId])
  .out(statusCode(StatusCode.Created))
  .errorOut(oneOf(serverErrorVariant))
  .zServerLogic(createGlobalHandler)

private def createGlobalHandler(reqBody: CreateIngredientReqBody):
  ZIO[CreateEnv, InternalServerError, IngredientId] =
  ZIO.serviceWithZIO[IngredientsRepo](_
    .addGlobal(reqBody.name)
    .map(_.id)
    .orElseFail(InternalServerError())
  )

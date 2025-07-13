package api.ingredients

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
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

private val create: ZServerEndpoint[CreateEnv, Any] =
  ingredientsEndpoint
    .post
    .in(jsonBody[CreateIngredientReqBody])
    .out(plainBody[IngredientId])
    .out(statusCode(StatusCode.Created))
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(createHandler)

private def createHandler(reqBody: CreateIngredientReqBody):
  ZIO[AuthenticatedUser & CreateEnv, InternalServerError, IngredientId] =
  ZIO.serviceWithZIO[IngredientsRepo](_
    .addCustom(reqBody.name)
    .map(_.id)
    .orElseFail(InternalServerError())
  )

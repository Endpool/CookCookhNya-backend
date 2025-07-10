package api.ingredients.personal

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.serverErrorVariant
import api.ingredients.CreateIngredientReqBody
import db.repositories.IngredientsRepo
import domain.{IngredientId, InternalServerError}

import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type CreateEnv = IngredientsRepo

private val createPersonal: ZServerEndpoint[CreateEnv, Any] =
  personalIngredientsEndpoint
    .post
    .in(jsonBody[CreateIngredientReqBody])
    .out(jsonBody[IngredientId])
    .out(statusCode(StatusCode.Created))
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(createPersonalHandler)

private def createPersonalHandler(reqBody: CreateIngredientReqBody):
  ZIO[AuthenticatedUser & CreateEnv, InternalServerError, IngredientId] =
  ZIO.serviceWithZIO[IngredientsRepo](_
    .addPersonal(reqBody.name)
    .map(_.id)
    .orElseFail(InternalServerError())
  )

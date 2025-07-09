package api.ingredients

import api.Authentication.{zSecuredServerLogic, AuthenticatedUser}
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

private val createPrivate: ZServerEndpoint[CreateEnv, Any] =
  endpoint
    .in("my" / "ingredients")
    .post
    .in(jsonBody[CreateIngredientReqBody])
    .out(jsonBody[IngredientId])
    .out(statusCode(StatusCode.Created))
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(createPrivateHandler)

private def createPrivateHandler(reqBody: CreateIngredientReqBody):
  ZIO[AuthenticatedUser & CreateEnv, InternalServerError, IngredientId] =
  ZIO.serviceWithZIO[IngredientsRepo](_
    .addPrivate(reqBody.name)
    .map(_.id)
    .orElseFail(InternalServerError())
  )

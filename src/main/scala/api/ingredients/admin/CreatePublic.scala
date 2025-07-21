package api.ingredients.admin

import api.EndpointErrorVariants.serverErrorVariant
import api.ingredients.CreateIngredientReqBody
import db.repositories.{IngredientsRepo}
import domain.{ IngredientId, InternalServerError}

import io.circe.generic.auto.*
import sttp.model.StatusCode.Created
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type CreatePublicEnv = IngredientsRepo

private val createPublic: ZServerEndpoint[CreatePublicEnv, Any] =
  adminIngredientsEndpoint
    .post
    .in(jsonBody[CreateIngredientReqBody])
    .out(plainBody[IngredientId] and statusCode(Created))
    .errorOut(oneOf(serverErrorVariant))
    .zServerLogic(createPublicHandler)

private def createPublicHandler(req: CreateIngredientReqBody):
  ZIO[CreatePublicEnv, InternalServerError, IngredientId] =
  ZIO.serviceWithZIO[IngredientsRepo](_
    .addPublic(req.name)
    .orElseFail(InternalServerError())
    .map(_.id)
  )

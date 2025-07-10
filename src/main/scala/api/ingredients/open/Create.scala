package api.ingredients.open

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



private[ingredients] type CreateEnv = IngredientsRepo

private[ingredients] val createPublic: ZServerEndpoint[CreateEnv, Any] =
  publicIngredientsEndpoint
  .post
  .in(jsonBody[CreateIngredientReqBody])
  .out(jsonBody[IngredientId])
  .out(statusCode(StatusCode.Created))
  .errorOut(oneOf(serverErrorVariant))
  .zServerLogic(createPublicHandler)

private def createPublicHandler(reqBody: CreateIngredientReqBody):
  ZIO[CreateEnv, InternalServerError, IngredientId] =
  ZIO.serviceWithZIO[IngredientsRepo] {
    _.add(reqBody.name).map(_.id)
  }.mapError(_ => InternalServerError())

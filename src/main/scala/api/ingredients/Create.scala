package api.ingredients

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{serverErrorVariant, userNotFoundVariant}
import db.repositories.{IngredientsRepo, UsersRepo}
import domain.{IngredientId, InternalServerError, UserNotFound}
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type CreateEnv = IngredientsRepo & UsersRepo

private val create: ZServerEndpoint[CreateEnv, Any] =
  ingredientsEndpoint
    .post
    .in(jsonBody[CreateIngredientReqBody])
    .out(plainBody[IngredientId])
    .out(statusCode(StatusCode.Created))
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(createHandler)

private def createHandler(reqBody: CreateIngredientReqBody):
  ZIO[AuthenticatedUser & CreateEnv, InternalServerError | UserNotFound, IngredientId] = for
  userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
  userNotFound <- ZIO.serviceWithZIO[UsersRepo](_.) 
  ZIO.serviceWithZIO[IngredientsRepo](_
    .addCustom(reqBody.name)
    .map(_.id)
    .orElseFail(InternalServerError())
  )


package api.ingredients

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.EndpointErrorVariants.{serverErrorVariant, userNotFoundVariant}
import db.repositories.{IngredientsRepo, UserQueries}
import domain.{IngredientId, InternalServerError, UserNotFound}
import db.QuillConfig.provideDS
import db.QuillConfig.ctx.*

import javax.sql.DataSource
import io.getquill.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

private type CreateEnv = IngredientsRepo & DataSource

private val create: ZServerEndpoint[CreateEnv, Any] =
  ingredientsEndpoint
    .post
    .in(jsonBody[CreateIngredientReqBody])
    .out(plainBody[IngredientId])
    .out(statusCode(StatusCode.Created))
    .errorOut(oneOf(serverErrorVariant, userNotFoundVariant))
    .zSecuredServerLogic(createHandler)

private def createHandler(reqBody: CreateIngredientReqBody):
  ZIO[AuthenticatedUser & CreateEnv, InternalServerError | UserNotFound, IngredientId] =
  for
    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    dataSource <- ZIO.service[DataSource]
    userNotFound <- run(UserQueries.getUserById(lift(userId)).isEmpty)
      .provideDS(using dataSource)
      .orElseFail(InternalServerError())
    _ <- ZIO.fail(UserNotFound(userId.toString))
      .when(userNotFound)
    
    ingredientId <- ZIO.serviceWithZIO[IngredientsRepo](_
      .addCustom(reqBody.name)
      .map(_.id)
      .orElseFail(InternalServerError())
    )
  yield ingredientId

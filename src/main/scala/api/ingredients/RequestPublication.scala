package api.ingredients

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.variantJson
import api.EndpointErrorVariants.{ingredientNotFoundVariant, serverErrorVariant}
import db.repositories.{
  IngredientPublicationRequestsRepo,
  IngredientsRepo, IngredientPublicationRequestsQueries
}
import domain.{IngredientId, IngredientNotFound, InternalServerError}
import db.tables.publication.DbPublicationRequestStatus.given
import db.QuillConfig.provideDS
import db.QuillConfig.ctx.*

import io.circe.generic.auto.*
import io.getquill.*
import javax.sql.DataSource
import sttp.model.StatusCode.BadRequest
import sttp.tapir.generic.auto.*
import sttp.tapir.ztapir.*
import zio.ZIO

private final case class IngredientAlreadyPublished(
  ingredientId: IngredientId,
  message: String = "Ingredient already published",
)
object IngredientAlreadyPublished:
  val variant = BadRequest.variantJson[IngredientAlreadyPublished]

private final case class IngredientAlreadyPending(
   ingredientId: IngredientId,
   message: String = "Ingredient already pending"
 )
object IngredientAlreadyPending:
  val variant = BadRequest.variantJson[IngredientAlreadyPending]

private type RequestPublicationEnv = IngredientPublicationRequestsRepo & IngredientsRepo & DataSource
private val requestPublication: ZServerEndpoint[RequestPublicationEnv, Any] =
  ingredientsEndpoint
    .post
    .in(path[IngredientId]("ingredientId") / "request-publication")
    .errorOut(oneOf(
      serverErrorVariant, IngredientAlreadyPublished.variant,
      IngredientAlreadyPending.variant, ingredientNotFoundVariant
    ))
    .zSecuredServerLogic(requestPublicationHandler)

def requestPublicationHandler(ingredientId: IngredientId):
  ZIO[
    AuthenticatedUser & RequestPublicationEnv,
    InternalServerError | IngredientAlreadyPublished | IngredientAlreadyPending | IngredientNotFound,
    Unit
  ] =
  for
    ingredient <- ZIO.serviceWithZIO[IngredientsRepo](_
      .get(ingredientId)
      .some.orElseFail(IngredientNotFound(ingredientId.toString))
    )

    _ <- ZIO.fail(IngredientAlreadyPublished(ingredientId))
    .when(ingredient.isPublished)

    dataSource <- ZIO.service[DataSource]
    alreadyPending <- run(
      IngredientPublicationRequestsQueries
        .pendingRequestsByIdQ(lift(ingredientId)).nonEmpty
    )
      .provideDS(using dataSource)
      .orElseFail(InternalServerError())
    _ <- ZIO.fail(IngredientAlreadyPending(ingredientId))
      .when(alreadyPending)

    _ <- ZIO.serviceWithZIO[IngredientPublicationRequestsRepo](_
    .requestPublication(ingredientId)
    .orElseFail(InternalServerError())
    )
  yield ()

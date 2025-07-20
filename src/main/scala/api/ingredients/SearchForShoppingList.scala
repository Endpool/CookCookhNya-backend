package api.ingredients

import api.Authentication.{AuthenticatedUser, zSecuredServerLogic}
import api.common.search.*
import api.EndpointErrorVariants.serverErrorVariant
import db.QuillConfig.provideDS
import db.repositories.IngredientsQueries
import db.tables.DbShoppingList
import domain.{IngredientId, InternalServerError}

import io.circe.generic.auto.*
import io.getquill.{query => quillQuery, *}
import javax.sql.DataSource
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.*
import zio.ZIO

final case class IngredientsForShoppingListResp(
  id: IngredientId,
  name: String,
  isInShopList: Boolean
) extends Searchable

private type SearchForShopListEnv = DataSource

private val searchForShoppingList: ZServerEndpoint[SearchForShopListEnv, Any] =
  ingredientsEndpoint(
    path="ingredients-for-shopping-list"
  ).get
    .in(SearchParams.query)
    .in(PaginationParams.query)
    .out(jsonBody[SearchResp[IngredientsForShoppingListResp]])
    .errorOut(oneOf(serverErrorVariant))
    .zSecuredServerLogic(searchForShoppingListHandler)

private def searchForShoppingListHandler(
  searchParams: SearchParams,
  paginationParams: PaginationParams,
): ZIO[AuthenticatedUser & SearchForShopListEnv,
       InternalServerError,
       SearchResp[IngredientsForShoppingListResp]] =
  import db.QuillConfig.ctx.*
  for
    dataSource <- ZIO.service[DataSource]
    userId <- ZIO.serviceWith[AuthenticatedUser](_.userId)
    allIngredientsAvailability <- run(
      IngredientsQueries.visibleIngredientsQ(lift(userId))
        .leftJoin(quillQuery[DbShoppingList])
        .on((i, ri) => i.id == ri.ingredientId && ri.ownerId == lift(userId))
        .map((i, ri) => IngredientsForShoppingListResp(i.id, i.name, ri.map(_.ownerId).isDefined))
    ).provideDS(using dataSource)
      .map(Vector.from)
      .orElseFail(InternalServerError())
    res = Searchable.search(allIngredientsAvailability, searchParams)
  yield SearchResp(res.paginate(paginationParams), res.length)

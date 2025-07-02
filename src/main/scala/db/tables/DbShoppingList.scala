package db.tables

import db.CustomSqlNameMapper
import domain.{UserId, IngredientId}

import com.augustnagro.magnum.*

@Table(PostgresDbType, CustomSqlNameMapper)
case class DbShoppingList(
  ownerId: UserId,
  ingredientId: IngredientId
) derives DbCodec

val shoppingListTable = TableInfo[DbShoppingList, DbShoppingList, Null]

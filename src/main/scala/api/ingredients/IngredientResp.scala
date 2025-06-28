package api.ingredients

import domain.IngredientId
import db.tables.DbIngredient

final case class IngredientResp(id: IngredientId, name: String)

object IngredientResp:
  def fromDb(dbIngredient: DbIngredient): IngredientResp =
    IngredientResp(dbIngredient.id, dbIngredient.name)

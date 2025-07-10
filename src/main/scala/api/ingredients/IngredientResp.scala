package api.ingredients

import api.common.search.Searchable
import domain.IngredientId
import db.tables.DbIngredient

final case class IngredientResp(id: IngredientId, name: String) extends Searchable

object IngredientResp:
  def fromDb(dbIngredient: DbIngredient): IngredientResp =
    IngredientResp(dbIngredient.id, dbIngredient.name)

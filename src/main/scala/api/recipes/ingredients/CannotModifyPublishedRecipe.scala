package api.recipes.ingredients

import api.variantJson
import domain.RecipeId

import io.circe.generic.auto.*
import sttp.model.StatusCode.Forbidden
import sttp.tapir.generic.auto.*

final case class CannotModifyPublishedRecipe(
  recipeId: RecipeId,
  message: String = "Cannot modify published recipe",
)

object CannotModifyPublishedRecipe:
  val variant = Forbidden.variantJson[CannotModifyPublishedRecipe]

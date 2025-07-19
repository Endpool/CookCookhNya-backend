package api.recipes

import domain.IngredientId

final case class CreateRecipeReqBody(
  name: String,
  sourceLink: Option[String],
  ingredients: List[IngredientId],
)


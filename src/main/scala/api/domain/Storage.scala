package api.domain

case class Storage(
                  ownerId: UserId,
                  members: List[UserId], 
                  ingredients: List[IngredientId]
                  )
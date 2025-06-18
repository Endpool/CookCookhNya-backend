package api.domain

case class Storage(
                  storageId: StorageId,
                  ownerId: UserId,
                  members: List[UserId], 
                  ingredients: List[IngredientId]
                  )
package api.domain

type StorageId = BaseId

case class Storage(
                  storageId: StorageId,
                  ownerId: UserId,
                  members: List[UserId],
                  ingredients: List[IngredientId]
                  )

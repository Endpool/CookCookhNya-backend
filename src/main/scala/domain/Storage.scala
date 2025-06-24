package domain

type StorageId = BaseId

case class Storage(
                  id: StorageId,
                  name: String,
                  ownerId: UserId,
                  members: Vector[UserId],
                  ingredients: Vector[IngredientId]
                  )

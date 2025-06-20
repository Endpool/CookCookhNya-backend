package api.domain

type StorageId = BaseId

case class Storage(
                  storageId: StorageId,
                  name: String,
                  ownerId: UserId,
                  members: List[UserId],
                  ingredients: List[IngredientId]
                  )

object Storage:
  final case class CreationEntity(name: String, ownerId: UserId)

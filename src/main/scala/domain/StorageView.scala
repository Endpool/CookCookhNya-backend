package domain

case class StorageView(
                    id: StorageId,
                    name: String,
                    ownerId: UserId,
                  )

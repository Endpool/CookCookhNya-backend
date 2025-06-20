package domain

case class StorageView(
                    storageId: StorageId,
                    name: String,
                    ownerId: UserId,
                  )

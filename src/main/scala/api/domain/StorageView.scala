package api.domain

case class StorageView(
                    storageId: StorageId,
                    name: String,
                    ownerId: UserId,
                  )

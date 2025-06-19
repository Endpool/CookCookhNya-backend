package domain

type UserId = BaseId

case class User(
               userId: UserId,
               username: String,
               storages: List[Storage]
               )

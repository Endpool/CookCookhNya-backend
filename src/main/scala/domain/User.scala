package domain

type UserId = BaseId

case class User(
               id: UserId,
               username: String,
               storages: Vector[Storage]
               )

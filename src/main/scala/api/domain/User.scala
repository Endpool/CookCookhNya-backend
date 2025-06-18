package api.domain

case class User(
               userId: UserId,
               username: String,
               storages: List[Storage]
               )
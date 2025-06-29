package api

import db.DbError.FailedDbQuery
import domain.{IngredientError, IngredientId, InternalServerError, StorageError, StorageId, UserError, UserId}
import zio.{IO, ZIO}

def handleFailedSqlQuery(error: FailedDbQuery): IO[InternalServerError, String] =
  val pattern = """Key \((.*)\)=\((.*)\) is not present in table "(.*)".""".r
  error.sqlExc.getServerErrorMessage.getDetail match
    case pattern(key, _, table) => ZIO.succeed(key)
    case _ => ZIO.fail(InternalServerError())

def toIngredientNotFound(keyName: String, ingredientId: IngredientId):
IO[IngredientError.NotFound, Unit] =
  if keyName == "ingredient_id" then ZIO.fail(IngredientError.NotFound(ingredientId))
  else ZIO.unit

def toStorageNotFound(keyName: String, storageId: StorageId):
  IO[StorageError.NotFound, Unit] =
  if keyName == "storage_id" then ZIO.fail(StorageError.NotFound(storageId))
  else ZIO.unit

def toUserNotFound(keyName: String, userId: UserId):
  IO[UserError.NotFound, Unit] =
  if keyName == "member_id" then ZIO.fail(UserError.NotFound(userId))
  else ZIO.unit

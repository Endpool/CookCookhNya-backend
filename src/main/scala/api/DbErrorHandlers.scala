package api

import db.DbError.FailedDbQuery
import domain.{IngredientError, InternalServerError, StorageError, UserError}
import zio.{IO, ZIO}
import db.tables.*

case class ForeignKeyViolation(keyName: String, keyValue: String, tableName: String)

def handleFailedSqlQuery(error: FailedDbQuery): IO[InternalServerError, (String, String, String)] =
  val pattern = """Key \((.*)\)=\((.*)\) is not present in table "(.*)".""".r
  pattern.findFirstMatchIn(error.sqlExc.getMessage) match
    case Some(text) =>
      val keyName = text.group(1)
      val keyValue = text.group(2)
      val tableName = text.group(3)
      ZIO.succeed((keyName, keyValue, tableName))
    case None => ZIO.fail(InternalServerError())

def failIfIngredientNotFound(keyName: String, ingredientId: String):
IO[IngredientError.NotFound, Unit] = {
  val sqlColName: String = storageIngredientsTable.ingredientId.sqlName
  if keyName == sqlColName then ZIO.fail(IngredientError.NotFound(ingredientId))
  else ZIO.unit
}

def failIfStorageNotFound(keyName: String, storageId: String):
  IO[StorageError.NotFound, Unit] =
  val sqlColName: String = storageMembersTable.storageId.sqlName
  if keyName == sqlColName then ZIO.fail(StorageError.NotFound(storageId))
  else ZIO.unit

def failIfUserNotFound(keyName: String, userId: String):
  IO[UserError.NotFound, Unit] = {
  val sqlColName: String = storageMembersTable.memberId.sqlName
  if keyName == sqlColName then ZIO.fail(UserError.NotFound(userId))
  else ZIO.unit
}

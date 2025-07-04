package api

import _root_.db.DbError.FailedDbQuery
import _root_.db.tables.*
import domain.{IngredientError, InternalServerError, StorageError, UserError}

import zio.{IO, ZIO}

case class ForeignKeyViolation(keyName: String, keyValue: String, tableName: String)

def handleFailedSqlQuery(error: FailedDbQuery): Option[ForeignKeyViolation] =
  val pattern = """Key \((.*)\)=\((.*)\) is not present in table "(.*)".""".r
  pattern.findFirstMatchIn(error.sqlExc.getMessage).map { text =>
    ForeignKeyViolation(
      keyName   = text.group(1),
      keyValue  = text.group(2),
      tableName = text.group(3),
    )
  }

def toIngredientNotFound(fkv: ForeignKeyViolation):
  Option[IngredientError.NotFound] =
  Option.when(fkv.keyName == storageIngredientsTable.ingredientId.sqlName)
    (IngredientError.NotFound(fkv.keyValue))

def toStorageNotFound(fkv: ForeignKeyViolation):
  Option[StorageError.NotFound] =
  Option.when(fkv.keyName == storageMembersTable.storageId.sqlName)
    (StorageError.NotFound(fkv.keyValue))

def toUserNotFound(fkv: ForeignKeyViolation):
  Option[UserError.NotFound] =
  Option.when(fkv.keyName == storageMembersTable.memberId.sqlName)
    (UserError.NotFound(fkv.keyValue))

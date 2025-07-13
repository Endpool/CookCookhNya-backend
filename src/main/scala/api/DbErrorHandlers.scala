package api

import _root_.db.DbError.FailedDbQuery
import _root_.db.tables.*
import domain.{IngredientNotFound, StorageNotFound, UserNotFound}

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
  Option[IngredientNotFound] =
  Option.when(fkv.keyName == storageIngredientsTable.ingredientId.sqlName)
    (IngredientNotFound(fkv.keyValue))

def toStorageNotFound(fkv: ForeignKeyViolation):
  Option[StorageNotFound] =
  Option.when(fkv.keyName == storageMembersTable.storageId.sqlName)
    (StorageNotFound(fkv.keyValue))

def toUserNotFound(fkv: ForeignKeyViolation):
  Option[UserNotFound] =
  Option.when(fkv.keyName == storageMembersTable.memberId.sqlName)
    (UserNotFound(fkv.keyValue))

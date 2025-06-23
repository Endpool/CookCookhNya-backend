package db

import com.augustnagro.magnum.magzio.*
import db.tables.*

def createTables(xa: Transactor) = {
  xa.transact {
    val tableList = List(
      // alias cannot be referenced with magnum DDL due to its option type
      sql"""
        CREATE TABLE IF NOT EXISTS ${Users.table}(
          ${Users.table.id} BIGINT PRIMARY KEY,
          alias VARCHAR(255),
          ${Users.table.fullName} VARCHAR(255) NOT NULL
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${DbIngredient.table}(
          ${DbIngredient.table.id} SERIAL PRIMARY KEY,
          ${DbIngredient.table.name} VARCHAR(255) NOT NULL
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${Storages.table}(
          ${Storages.table.id} SERIAL PRIMARY KEY,
          ${Storages.table.ownerId} BIGINT NOT NULL,
          ${Storages.table.name} VARCHAR(255) NOT NULL,
          FOREIGN KEY (${Storages.table.ownerId}) REFERENCES ${Users.table}(${Users.table.id}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${StorageMembers.table}(
          ${StorageMembers.table.storageId} INT NOT NULL,
          ${StorageMembers.table.memberId} BIGINT NOT NULL,
          PRIMARY KEY (${StorageMembers.table.storageId}, ${StorageMembers.table.memberId}),
          FOREIGN KEY (${StorageMembers.table.storageId}) REFERENCES ${Storages.table}(${Storages.table.id}) ON DELETE CASCADE,
          FOREIGN KEY (${StorageMembers.table.memberId}) REFERENCES ${Users.table}(${Users.table.id}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${StorageIngredients.table}(
          ${StorageIngredients.table.storageId} INT NOT NULL,
          ${StorageIngredients.table.ingredientId} INT NOT NULL,
          PRIMARY KEY (${StorageIngredients.table.storageId}, ${StorageIngredients.table.ingredientId}),
          FOREIGN KEY (${StorageIngredients.table.storageId}) REFERENCES ${Storages.table}(${Storages.table.id}) ON DELETE CASCADE,
          FOREIGN KEY (${StorageIngredients.table.ingredientId}) REFERENCES ${DbIngredient.table}(${DbIngredient.table.id}) ON DELETE CASCADE
        );
      """
    )

    tableList.map(_.update.run())
  }
}

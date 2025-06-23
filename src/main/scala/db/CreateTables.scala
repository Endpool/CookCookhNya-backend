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
        CREATE TABLE IF NOT EXISTS ${ingredientsTable}(
          ${ingredientsTable.id} SERIAL PRIMARY KEY,
          ${ingredientsTable.name} VARCHAR(255) NOT NULL
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${storagesTable}(
          ${storagesTable.id} SERIAL PRIMARY KEY,
          ${storagesTable.ownerId} BIGINT NOT NULL,
          ${storagesTable.name} VARCHAR(255) NOT NULL,
          FOREIGN KEY (${storagesTable.ownerId}) REFERENCES ${Users.table}(${Users.table.id}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${StorageMembers.table}(
          ${StorageMembers.table.storageId} INT NOT NULL,
          ${StorageMembers.table.memberId} BIGINT NOT NULL,
          PRIMARY KEY (${StorageMembers.table.storageId}, ${StorageMembers.table.memberId}),
          FOREIGN KEY (${StorageMembers.table.storageId}) REFERENCES ${storagesTable}(${storagesTable.id}) ON DELETE CASCADE,
          FOREIGN KEY (${StorageMembers.table.memberId}) REFERENCES ${Users.table}(${Users.table.id}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${storageIngredientsTable}(
          ${storageIngredientsTable.storageId} INT NOT NULL,
          ${storageIngredientsTable.ingredientId} INT NOT NULL,
          PRIMARY KEY (${storageIngredientsTable.storageId}, ${storageIngredientsTable.ingredientId}),
          FOREIGN KEY (${storageIngredientsTable.storageId}) REFERENCES ${storagesTable}(${storagesTable.id}) ON DELETE CASCADE,
          FOREIGN KEY (${storageIngredientsTable.ingredientId}) REFERENCES ${ingredientsTable}(${ingredientsTable.id}) ON DELETE CASCADE
        );
      """
    )

    tableList.map(_.update.run())
  }
}

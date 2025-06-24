package db

import db.tables.*

import com.augustnagro.magnum.magzio.*

def createTables(xa: Transactor) = {
  xa.transact {
    val tableList = List(
      // alias cannot be referenced with magnum DDL due to its option type
      sql"""
        CREATE TABLE IF NOT EXISTS ${usersTable}(
          ${usersTable.id} BIGINT PRIMARY KEY,
          alias VARCHAR(255),
          ${usersTable.fullName} VARCHAR(255) NOT NULL
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
          FOREIGN KEY (${storagesTable.ownerId}) REFERENCES ${usersTable}(${usersTable.id}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${storageMembersTable}(
          ${storageMembersTable.storageId} INT NOT NULL,
          ${storageMembersTable.memberId} BIGINT NOT NULL,
          PRIMARY KEY (${storageMembersTable.storageId}, ${storageMembersTable.memberId}),
          FOREIGN KEY (${storageMembersTable.storageId}) REFERENCES ${storagesTable}(${storagesTable.id}) ON DELETE CASCADE,
          FOREIGN KEY (${storageMembersTable.memberId}) REFERENCES ${usersTable}(${usersTable.id}) ON DELETE CASCADE
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

package api.db

import com.augustnagro.magnum.magzio.*
import api.db.tables.*

def createTables(xa: Transactor) = {
  xa.transact {
    val tableList = List(
      sql"""
        CREATE TABLE IF NOT EXISTS ${Users.table}(
          ${Users.table.userId} SERIAL PRIMARY KEY,
          ${Users.table.username} VARCHAR(255) UNIQUE NOT NULL
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${Ingredients.table}(
          ${Ingredients.table.ingredientId} SERIAL PRIMARY KEY,
          ${Ingredients.table.name} VARCHAR(255) NOT NULL
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${Storages.table}(
          ${Storages.table.storageId} SERIAL PRIMARY KEY,
          ${Storages.table.ownerId} INT NOT NULL,
          FOREIGN KEY (${Storages.table.ownerId}) REFERENCES ${Users.table}(${Users.table.userId}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${StorageMembers.table}(
          ${StorageMembers.table.storageId} INT NOT NULL,
          ${StorageMembers.table.ownerId} INT NOT NULL,
          PRIMARY KEY (${StorageMembers.table.storageId}, ${StorageMembers.table.ownerId}),
          FOREIGN KEY (${StorageMembers.table.storageId}) REFERENCES ${Storages.table}(${Storages.table.storageId}) ON DELETE CASCADE,
          FOREIGN KEY (${StorageMembers.table.ownerId}) REFERENCES ${Users.table}(${Users.table.userId}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${StorageIngredients.table}(
          ${StorageIngredients.table.storageId} INT NOT NULL,
          ${StorageIngredients.table.ingredientId} INT NOT NULL,
          PRIMARY KEY (${StorageIngredients.table.storageId}, ${StorageIngredients.table.ingredientId}),
          FOREIGN KEY (${StorageIngredients.table.storageId}) REFERENCES ${Storages.table}(${Storages.table.storageId}) ON DELETE CASCADE,
          FOREIGN KEY (${StorageIngredients.table.ingredientId}) REFERENCES ${Ingredients.table}(${Ingredients.table.ingredientId}) ON DELETE CASCADE
        );
      """
    )

    tableList.map(_.update.run())
  }
}

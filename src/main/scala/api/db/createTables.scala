package api.db

import com.augustnagro.magnum.magzio.*
import api.db.tables.*

def createTables(xa: Transactor) = {
  xa.transact {
    val tableList = List(
      sql"""
          CREATE TABLE IF NOT EXISTS ${Users.table}(
            ${Users.table.userId} SERIAL PRIMARY KEY,
            ${Users.table.username} VARCHAR(255) UNIQUE NOT NULL,
          )
          """,

      sql"""
          CREATE TABLE IF NOT EXISTS ${Ingredients.table}(
            ${Ingredients.table.ingredientId} SERIAL PRIMARY KEY,
            ${Ingredients.table.name} VARCHAR(255) NOT NULL
          )
          """,

      sql"""
          CREATE TABLE IF NOT EXISTS ${Storages.table}(
            ${Storages.table.storageId} SERIAL PRIMARY KEY,
            ${Storages.table.ownerId} INT NOT NULL REFERENCES ${tables.Users.table}(${tables.Users.table.userId}),
          )
          """,

      sql"""
           CREATE TABLE IF NOT EXISTS ${StorageMembers.table}(
            ${StorageMembers.table.storageId} INT NOT NULL,
            ${StorageMembers.table.ownerId} INT NOT NULL )
           """,

    sql"""
           CREATE TABLE IF NOT EXISTS ${StorageIngredients.table}(
            ${StorageIngredients.table.storageId} INT NOT NULL,
            ${StorageIngredients.table.ingredientId} INT NOT NULL )
           """
    )
    
    tableList.map(_.update.run())
  }
}
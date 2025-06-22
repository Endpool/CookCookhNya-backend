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
        CREATE TABLE IF NOT EXISTS ${Ingredients.table}(
          ${Ingredients.table.id} SERIAL PRIMARY KEY,
          ${Ingredients.table.name} VARCHAR(255) NOT NULL
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${Storages.table}(
          ${Storages.table.id} SERIAL PRIMARY KEY,
          ${Storages.table.ownerId} INT NOT NULL,
          ${Storages.table.name} VARCHAR(255) NOT NULL,
          FOREIGN KEY (${Storages.table.ownerId}) REFERENCES ${Users.table}(${Users.table.id}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS ${StorageMembers.table}(
          ${StorageMembers.table.storageId} INT NOT NULL,
          ${StorageMembers.table.memberId} INT NOT NULL,
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
          FOREIGN KEY (${StorageIngredients.table.ingredientId}) REFERENCES ${Ingredients.table}(${Ingredients.table.id}) ON DELETE CASCADE
        );
      """,

      sql"""
           CREATE TABLE IF NOT EXISTS ${Recipes.table}(
            ${Recipes.table.id} SERIAL PRIMARY KEY,
            ${Recipes.table.name} VARCHAR(255) NOT NULL,
            ${Recipes.table.sourceLink} VARCHAR(128) NOT NULL
           );
           """,

      sql"""
           CREATE TABLE IF NOT EXISTS ${RecipeIngredients.table}(
            ${RecipeIngredients.table.recipeId} INT NOT NULL,
            ${RecipeIngredients.table.ingredientId} INT NOT NULL,
            PRIMARY KEY (${RecipeIngredients.table.recipeId}, ${StorageIngredients.table.ingredientId}),
            FOREIGN KEY (${RecipeIngredients.table.recipeId}) REFERENCES ${Recipes.table}(${Recipes.table.id}) ON DELETE CASCADE,
            FOREIGN KEY (${RecipeIngredients.table.ingredientId}) REFERENCES ${Ingredients.table}(${Ingredients.table.id}) ON DELETE CASCADE
           );
           """
    )

    tableList.map(_.update.run())
  }
}

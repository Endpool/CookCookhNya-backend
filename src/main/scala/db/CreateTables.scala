package db

import db.tables.*
import com.augustnagro.magnum.magzio.*
import com.augustnagro.magnum.FragWriter
import db.tables.publication.{DbPublicationRequestStatus, DbRecipePublicationRequest}

def createTables(xa: Transactor) =
  def sql(str: String) = Frag(str, Seq.empty, FragWriter.empty)
  xa.transact {
    val tableList = List(
      sql"""
        CREATE EXTENSION IF NOT EXISTS pgcrypto
      """,

      sql(DbPublicationRequestStatus.createType),

      // alias cannot be referenced with magnum DDL due to its option type
      sql"""
        CREATE TABLE IF NOT EXISTS $usersTable(
          ${usersTable.id} BIGINT PRIMARY KEY,
          alias VARCHAR(255),
          ${usersTable.fullName} VARCHAR(255) NOT NULL
        );
      """,

      sql(DbIngredient.createTable),

      sql"""
        CREATE TABLE IF NOT EXISTS $storagesTable(
          ${storagesTable.id} UUID PRIMARY KEY DEFAULT gen_random_uuid(),
          ${storagesTable.ownerId} BIGINT NOT NULL,
          ${storagesTable.name} VARCHAR(255) NOT NULL,
          FOREIGN KEY (${storagesTable.ownerId}) REFERENCES $usersTable(${usersTable.id}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS $storageMembersTable(
          ${storageMembersTable.storageId} UUID NOT NULL,
          ${storageMembersTable.memberId} BIGINT NOT NULL,
          PRIMARY KEY (${storageMembersTable.storageId}, ${storageMembersTable.memberId}),
          FOREIGN KEY (${storageMembersTable.storageId}) REFERENCES $storagesTable(${storagesTable.id}) ON DELETE CASCADE,
          FOREIGN KEY (${storageMembersTable.memberId}) REFERENCES $usersTable(${usersTable.id}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS $storageIngredientsTable(
          ${storageIngredientsTable.storageId} UUID NOT NULL,
          ${storageIngredientsTable.ingredientId} UUID NOT NULL,
          PRIMARY KEY (${storageIngredientsTable.storageId}, ${storageIngredientsTable.ingredientId}),
          FOREIGN KEY (${storageIngredientsTable.storageId}) REFERENCES $storagesTable(${storagesTable.id}) ON DELETE CASCADE,
          FOREIGN KEY (${storageIngredientsTable.ingredientId}) REFERENCES $ingredientsTable(${ingredientsTable.id}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS $recipesTable(
          ${recipesTable.id} UUID PRIMARY KEY DEFAULT gen_random_uuid(),
          ${recipesTable.name} VARCHAR(255) NOT NULL,
          ${recipesTable.creatorId} BIGINT NOT NULL,
          ${recipesTable.isPublished} BOOLEAN NOT NULL,
          ${recipesTable.sourceLink} VARCHAR(255),
          FOREIGN KEY (${recipesTable.creatorId}) REFERENCES $usersTable(${usersTable.id}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS $recipeIngredientsTable(
          ${recipeIngredientsTable.recipeId} UUID NOT NULL,
          ${recipeIngredientsTable.ingredientId} UUID NOT NULL,
          PRIMARY KEY (${recipeIngredientsTable.recipeId}, ${storageIngredientsTable.ingredientId}),
          FOREIGN KEY (${recipeIngredientsTable.recipeId}) REFERENCES $recipesTable(${recipesTable.id}) ON DELETE CASCADE,
          FOREIGN KEY (${recipeIngredientsTable.ingredientId}) REFERENCES $ingredientsTable(${ingredientsTable.id}) ON DELETE CASCADE
        );
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS $shoppingListTable(
          ${shoppingListTable.ownerId} BIGINT NOT NULL,
          ${shoppingListTable.ingredientId} UUID NOT NULL,
          PRIMARY KEY (${shoppingListTable.ownerId}, ${shoppingListTable.ingredientId}),
          FOREIGN KEY (${shoppingListTable.ownerId}) REFERENCES $usersTable(${usersTable.id}) ON DELETE CASCADE,
          FOREIGN KEY (${shoppingListTable.ingredientId}) REFERENCES $ingredientsTable(${ingredientsTable.id}) ON DELETE CASCADE
        )
      """,

      sql"""
        CREATE TABLE IF NOT EXISTS $storageInvitationTable(
          ${storageInvitationTable.storageId} UUID NOT NULL,
          ${storageInvitationTable.invitation} VARCHAR(255) NOT NULL,
          PRIMARY KEY (${storageInvitationTable.storageId}, ${storageInvitationTable.invitation}),
          FOREIGN KEY (${storageInvitationTable.storageId}) REFERENCES $storagesTable(${storagesTable.id}) ON DELETE CASCADE
        )
      """,
      sql(DbRecipePublicationRequest.createTable),
    )

    tableList.map(_.update.run())
  }

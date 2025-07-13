package api

import _root_.db.repositories.{
  IngredientsRepo,
  InvitationsRepo,
  RecipeIngredientsRepo,
  RecipesDomainRepo,
  RecipesRepo,
  RecipePublicationRequestsRepo,
  ShoppingListsRepo,
  StorageIngredientsRepo,
  StorageMembersRepo,
  StoragesRepo,
  UsersRepo,
}

import com.augustnagro.magnum.magzio.Transactor
import javax.sql.DataSource

type AppEnv
  = Transactor
  & DataSource
  & IngredientsRepo
  & InvitationsRepo
  & RecipeIngredientsRepo
  & RecipePublicationRequestsRepo
  & RecipesDomainRepo
  & RecipesRepo
  & ShoppingListsRepo
  & StorageIngredientsRepo
  & StorageMembersRepo
  & StoragesRepo
  & UsersRepo

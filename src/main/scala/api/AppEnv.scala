package api

import _root_.db.repositories.{
  IngredientsRepo,
  InvitationsRepo,
  RecipeIngredientsRepo,
  RecipesDomainRepo,
  RecipesRepo,
  ShoppingListsRepo,
  StorageIngredientsRepo,
  StorageMembersRepo,
  StoragesRepo,
  UsersRepo,
}

import com.augustnagro.magnum.magzio.Transactor

type AppEnv
  = Transactor
  & IngredientsRepo
  & InvitationsRepo
  & RecipeIngredientsRepo
  & RecipesDomainRepo
  & RecipesRepo
  & ShoppingListsRepo
  & StorageIngredientsRepo
  & StorageMembersRepo
  & StoragesRepo
  & UsersRepo

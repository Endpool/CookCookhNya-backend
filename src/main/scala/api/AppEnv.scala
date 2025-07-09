package api

import _root_.db.repositories.{
  IngredientsRepo,
  StorageIngredientsRepo,
  StorageMembersRepo,
  StoragesRepo,
  UsersRepo,
  RecipeIngredientsRepo,
  RecipesRepo,
  RecipesDomainRepo,
  ShoppingListsRepo,
  InvitationsRepo
}

import com.augustnagro.magnum.magzio.Transactor

type AppEnv
  = Transactor
  & IngredientsRepo
  & InvitationsRepo & InvitationsSecretKey
  & RecipeIngredientsRepo
  & RecipesDomainRepo
  & RecipesRepo
  & ShoppingListsRepo
  & StorageIngredientsRepo
  & StorageMembersRepo
  & StoragesRepo
  & UsersRepo

final case class InvitationsSecretKey(value: String)

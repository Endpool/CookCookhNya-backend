package api

import _root_.db.repositories.{
  IngredientsRepo,
  StorageIngredientsRepo,
  StorageMembersRepo,
  StoragesRepo,
  UsersRepo,
  RecipeIngredientsRepo,
  RecipesRepo
}

import com.augustnagro.magnum.magzio.Transactor

type AppEnv
  = Transactor
  & IngredientsRepo
  & StorageIngredientsRepo
  & StorageMembersRepo
  & StoragesRepo
  & UsersRepo
  & RecipeIngredientsRepo
  & RecipesRepo

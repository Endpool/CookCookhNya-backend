package api

import _root_.db.repositories.{
  IngredientsRepo,
  StorageIngredientsRepo,
  StorageMembersRepo,
  StoragesRepo,
  UsersRepo,
  RecipeIngredientsRepo,
  RecipesRepo,
  RecipesDomainRepo
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
  & RecipesDomainRepo

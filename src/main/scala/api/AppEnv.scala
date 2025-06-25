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

type AppEnv
= IngredientsRepo
  & StorageIngredientsRepo
  & StorageMembersRepo
  & StoragesRepo
  & UsersRepo
  & RecipeIngredientsRepo
  & RecipesRepo
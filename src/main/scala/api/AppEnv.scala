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

type AppEnv
= IngredientsRepo
  & StorageIngredientsRepo
  & StorageMembersRepo
  & StoragesRepo
  & UsersRepo
  & RecipeIngredientsRepo
  & RecipesRepo
  & RecipesDomainRepo
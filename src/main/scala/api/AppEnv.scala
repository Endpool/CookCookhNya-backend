package api

import api.db.repositories.{
  IIngredientRepo,
  IStorageIngredientsRepo,
  IStorageMembersRepo,
  IStoragesRepo
}

type AppEnv = IIngredientRepo &
              IStorageIngredientsRepo &
              IStorageMembersRepo &
              IStoragesRepo

package api

import api.db.repositories.{
  IIngredientRepo,
  IStorageIngredientsReopo,
  IStorageMembersRepo,
  IStoragesRepo
}

type AppEnv = IIngredientRepo &
              IStorageIngredientsReopo &
              IStorageMembersRepo &
              IStoragesRepo

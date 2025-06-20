package api

import api.db.repositories.{
  IIngredientRepo,
  IStorageIngredientsReopo,
  IStorageMembersRepo,
  IStorageRepo
}

type AppEnv = IIngredientRepo & 
              IStorageIngredientsReopo & 
              IStorageMembersRepo & 
              IStorageRepo

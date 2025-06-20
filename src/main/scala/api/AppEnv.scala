package api

import api.db.repositories.{
  IngredientRepoInterface,
  StorageIngredientsRepoInterface,
  StorageMembersRepoInterface,
  StorageRepoInterface
}

type AppEnv = IngredientRepoInterface & 
              StorageIngredientsRepoInterface & 
              StorageMembersRepoInterface & 
              StorageRepoInterface

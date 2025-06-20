package api

import db.repositories.{
  IIngredientsRepo,
  IStorageIngredientsRepo,
  IStorageMembersRepo,
  IStoragesRepo
}

type AppEnv
  = IIngredientsRepo
  & IStorageIngredientsRepo
  & IStorageMembersRepo
  & IStoragesRepo

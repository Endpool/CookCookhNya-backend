package api

import _root_.db.repositories.{
  IIngredientsRepo,
  IStorageIngredientsRepo,
  IStorageMembersRepo,
  IStoragesRepo,
  UsersRepo
}

type AppEnv
  = IIngredientsRepo
  & IStorageIngredientsRepo
  & IStorageMembersRepo
  & IStoragesRepo
  & UsersRepo

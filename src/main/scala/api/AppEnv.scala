package api

import _root_.db.repositories.{
  IIngredientsRepo,
  IStorageIngredientsRepo,
  IStorageMembersRepo,
  StoragesRepo,
  UsersRepo
}

type AppEnv
  = IIngredientsRepo
  & IStorageIngredientsRepo
  & IStorageMembersRepo
  & StoragesRepo
  & UsersRepo

package api

import _root_.db.repositories.{
  IIngredientsRepo,
  IStorageIngredientsRepo,
  StorageMembersRepo,
  StoragesRepo,
  UsersRepo
}

type AppEnv
  = IIngredientsRepo
  & IStorageIngredientsRepo
  & StorageMembersRepo
  & StoragesRepo
  & UsersRepo

package api

import _root_.db.repositories.{
  IIngredientsRepo,
  StorageIngredientsRepo,
  StorageMembersRepo,
  StoragesRepo,
  UsersRepo
}

type AppEnv
  = IIngredientsRepo
  & StorageIngredientsRepo
  & StorageMembersRepo
  & StoragesRepo
  & UsersRepo

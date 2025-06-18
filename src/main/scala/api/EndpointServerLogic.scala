package api

import zio.{ZIO, IO, UIO}

import api.domain.*
import api.Endpoints.CreateStorageReqBody
import api.Endpoints.StorageSummary

val createIngredient: Ingredient => UIO[Unit] =
  case Ingredient(ingredientId, ingredientName) => ZIO.succeed(())

val getIngredient:
  IngredientId => IO[IngredientError.NotFound, Ingredient] =
  ingredientId => ZIO.fail(IngredientError.NotFound(ingredientId))

val getAllIngredients:
  UIO[List[Ingredient]] =
  ZIO.succeed(Nil)

val deleteIngredient:
  IngredientId => IO[IngredientError.NotFound, Unit] =
  ingredientId => ZIO.succeed(())

val addIngredientToStorage:
  UserId => ((StorageId, IngredientId)) => IO[StorageError | IngredientError, Unit] =
  userId =>
    case (storageId, ingredientId) => ZIO.succeed(())

val deleteMyIngredientFromStorage:
  UserId => ((StorageId, IngredientId)) => IO[StorageError | IngredientError, Unit] =
  userId =>
    case (2, _) => ZIO.fail(StorageError.NotFound(2))
    case (_, 4) => ZIO.fail(IngredientError.NotFound(4))
    case _ => ZIO.unit

val getStorageIngredients:
  UserId => StorageId => IO[StorageError, List[IngredientId]] =
  userId => storageId => ZIO.succeed(Nil)

val getStorages:
  UserId => Unit => UIO[List[StorageSummary]] =
  userId => unit => ZIO.succeed(Nil)

val createStorage:
  UserId => CreateStorageReqBody => UIO[Storage] =
  userId =>
    case CreateStorageReqBody(name) => ZIO.succeed(Storage(1, name, 2, Nil, Nil))

val deleteStorage:
  UserId => StorageId => IO[StorageError.NotFound, Unit] =
  userId => storageId => ZIO.unit

val getStorageName:
  UserId => StorageId => IO[StorageError.NotFound, String] =
  userId => storageId => ZIO.succeed("placeholder")

val getStorageMembers:
  UserId => StorageId => IO[StorageError.NotFound, List[UserId]] =
  userId => storageId => ZIO.succeed(Nil)


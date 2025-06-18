package api

import zio.{ZIO, IO, UIO}

import api.domain.*
import api.Endpoints.CreateStorageReqBody

val createIngredient: Ingredient => UIO[Unit] =
  case Ingredient(ingredientId, ingredientName) => ZIO.succeed(())

val getIngredient:
  IngredientId => IO[IngredientError.NotFound, Ingredient] =
  ingredientId => ZIO.fail(IngredientError.NotFound(ingredientId))

val getAllIngredients: UIO[List[Ingredient]] =
  ZIO.succeed(Nil)

val deleteIngredient:
  IngredientId => IO[IngredientError.NotFound, Unit] =
  ingredientId => ZIO.succeed(())

val addIngredientToStorage:
  ((StorageId, IngredientId)) => IO[StorageError | IngredientError, Unit] =
  case (storageId, ingredientId) => ZIO.succeed(())

val deleteMyIngredientFromStorage:
  ((StorageId, IngredientId)) => IO[StorageError | IngredientError, Unit] =
  case (2, _) => ZIO.fail(StorageError.NotFound(2))
  case (_, 4) => ZIO.fail(IngredientError.NotFound(4))
  case _ => ZIO.unit

val getStorageIngredients:
  StorageId => IO[StorageError, List[IngredientId]] =
  storageId => ZIO.succeed(Nil)

val createStorage:
  CreateStorageReqBody => UIO[Storage] =
  case CreateStorageReqBody(name) => ZIO.succeed(Storage(1, name, 2, Nil, Nil))

val deleteStorage:
  StorageId => IO[StorageError.NotFound, Unit] =
  storageId => ZIO.unit

val getStorageName:
  StorageId => IO[StorageError.NotFound, String] =
  storageId => ZIO.succeed("placeholder")

val getStorageMembers:
  StorageId => IO[StorageError.NotFound, List[UserId]] =
  storageId => ZIO.succeed(Nil)


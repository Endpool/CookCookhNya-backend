package api

import zio.{ZIO, IO, UIO}

import api.domain.*

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
  case _ => ZIO.succeed(())

val getStorageIngredients:
  StorageId => IO[StorageError, List[IngredientId]] =
  storageId => ZIO.succeed(Nil)

package backend

import zio.{ZIO, IO, UIO}

val createIngredient: Ingredient => UIO[Unit] =
  case Ingredient(ingredientId, ingredientName) => ZIO.succeed(())

val getIngredient: IngredientId => IO[IngredientError, Ingredient] =
  ingredientId => ZIO.fail(IngredientNotFound())

val getAllIngredients: UIO[List[Ingredient]] =
  ZIO.succeed(Nil)

val deleteIngredient: IngredientId => IO[IngredientError, Unit] =
  ingredientId => ZIO.succeed(())

val addIngredientToStorage: ((StorageId, IngredientId)) => IO[StorageError | IngredientError, Unit] =
  case (storageId, ingredientId) => ZIO.succeed(())

val deleteMyIngredientFromStorage: ((StorageId, IngredientId)) => IO[StorageError | IngredientError, Unit] =
  case (storageId, ingredientId) => ZIO.succeed(())

val getAvailableIngredientsFromStorage:
  ((StorageId, IngredientId)) => IO[StorageError, List[Ingredient]] =
  case (storageId, ingredientId) => ZIO.succeed(Nil)

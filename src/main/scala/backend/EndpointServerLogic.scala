package backend

import zio.{ZIO, IO, UIO}

val createIngredient: Ingredient => UIO[Unit] =
  case Ingredient(ingredientId, ingredientName) => ZIO.succeed(())

val getIngredient: IngredientId => IO[IngredientError.IngredientNotFound, Ingredient] =
  id => ZIO.fail(IngredientError.IngredientNotFound(id))

val getAllIngredients: UIO[List[Ingredient]] =
  ZIO.succeed(Nil)

val deleteIngredient: IngredientId => IO[IngredientError.IngredientNotFound, Unit] =
  ingredientId => ZIO.succeed(())

val addIngredientToStorage: ((StorageId, IngredientId)) => IO[StorageError | IngredientError, Unit] =
  case (storageId, ingredientId) => ZIO.succeed(())

val deleteMyIngredientFromStorage: ((StorageId, IngredientId)) => IO[StorageError | IngredientError, Unit] =
  case (2, _) => ZIO.fail(StorageError.StorageNotFound(2))
  case (_, 4) => ZIO.fail(IngredientError.IngredientNotFound(4))
  case _ => ZIO.succeed(())

val getAvailableIngredientsFromStorage:
  ((StorageId, IngredientId)) => IO[StorageError, List[Ingredient]] =
  case (storageId, ingredientId) => ZIO.succeed(Nil)

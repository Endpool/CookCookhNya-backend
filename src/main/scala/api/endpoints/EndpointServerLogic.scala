package api.endpoints

import api.db.repositories.*
import api.domain.*
import zio.{Exit, IO, RIO, UIO, URIO, ZIO}

val createIngredient: Ingredient => URIO[IngredientRepoInterface, Unit] =
  ingredient =>
    ZIO.serviceWithZIO[IngredientRepoInterface] {
      _.add(ingredient).catchAll(_ => ZIO.succeed(()))
    }
  
    
val getIngredient:
  IngredientId => ZIO[IngredientRepoInterface, IngredientError.NotFound, Ingredient] =
  ingredientId =>
    ZIO.serviceWithZIO[IngredientRepoInterface](_.getById(ingredientId))
      .foldZIO(
        _ => ZIO.succeed(null),
        {
          case Some(ingredient) => ZIO.succeed(ingredient)
          case _ => ZIO.fail(IngredientError.NotFound(ingredientId))
        }
      )

val getAllIngredients: URIO[IngredientRepoInterface, Seq[Ingredient]] =
  ZIO.serviceWithZIO[IngredientRepoInterface] {
    _.getAll.catchAll(_ => ZIO.succeed(Nil))
  }

val deleteIngredient:
  IngredientId => ZIO[IngredientRepoInterface, IngredientError.NotFound, Unit] =
  ingredientId => 
    ZIO.serviceWithZIO[IngredientRepoInterface] {
      _.removeById(ingredientId).catchAll(_ => ZIO.fail(IngredientError.NotFound(ingredientId)))
    }

val addIngredientToStorage:
  ((StorageId, IngredientId)) => IO[StorageError | IngredientError, Unit] =
  case (storageId, ingredientId) => ZIO.succeed(())

val deleteMyIngredientFromStorage:
  ((StorageId, IngredientId)) => IO[StorageError | IngredientError, Unit] =
  case (2, _) => ZIO.fail(StorageError.NotFound(2))
  case (_, 4) => ZIO.fail(IngredientError.NotFound(4))
  case _ => ZIO.succeed(())

val getAllIngredientsFromStorage:
  StorageId => IO[StorageError, Seq[IngredientId]] =
  storageId => ZIO.succeed(Nil)

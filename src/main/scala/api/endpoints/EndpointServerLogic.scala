package api.endpoints

import api.db.repositories.*
import api.domain.*
import api.endpoints.StorageEndpoints.CreateStorageReqBody
import api.endpoints.StorageEndpoints.StorageSummary
import zio.{IO, RIO, UIO, URIO, ZIO}

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
  UserId => ((StorageId, IngredientId)) => ZIO[IngredientRepoInterface, StorageError | IngredientError, Unit] =
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

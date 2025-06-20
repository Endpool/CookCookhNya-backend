package api.endpoints

import api.db.repositories.*
import api.domain.*
import api.endpoints.StorageEndpoints.CreateStorageReqBody
import api.AppEnv

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
  UserId => ((StorageId, IngredientId)) => ZIO[StorageIngredientsRepoInterface, StorageError | IngredientError, Unit] =
  userId =>
    case (storageId, ingredientId) =>
      ZIO.serviceWithZIO[StorageIngredientsRepoInterface] {
        _.addIngredientToStorage(storageId, ingredientId)
      }

val deleteMyIngredientFromStorage:
  UserId => ((StorageId, IngredientId)) => ZIO[StorageIngredientsRepoInterface, StorageError | IngredientError, Unit] =
  userId =>
    case (storageId, ingredientId) =>
      ZIO.serviceWithZIO[StorageIngredientsRepoInterface] {
        _.removeIngredientFromStorageById(storageId, ingredientId)
      }

val getStorageIngredients:
  UserId => StorageId => ZIO[StorageIngredientsRepoInterface, StorageError, Seq[IngredientId]] =
  userId => storageId =>
    ZIO.serviceWithZIO[StorageIngredientsRepoInterface] {
      _.getAllIngredientsFromStorage(storageId)
    }

val getStorages:
  UserId => Unit => URIO[StorageRepoInterface, Seq[StorageView]] =
  userId => unit => ZIO.serviceWithZIO[StorageRepoInterface](_.getAllStorageViews)

val createStorage:
  UserId => CreateStorageReqBody => URIO[StorageRepoInterface, Storage] =
  userId =>
    case CreateStorageReqBody(name) =>
      ZIO.serviceWithZIO[StorageRepoInterface](_.createEmpty(name, userId))

val deleteStorage:
  UserId => StorageId => ZIO[StorageRepoInterface, StorageError.NotFound, Unit] =
  userId => storageId => ZIO.serviceWithZIO[StorageRepoInterface](_.removeById(storageId))

val getStorageView:
  UserId => StorageId => ZIO[StorageRepoInterface, StorageError.NotFound, StorageView] =
  userId => storageId =>
    ZIO.serviceWithZIO[StorageRepoInterface] {
      _.getStorageViewById(storageId)
    }

val getStorageMembers:
  UserId => StorageId => ZIO[StorageMembersRepoInterface, StorageError.NotFound, Seq[UserId]] =
  userId => storageId =>
    ZIO.serviceWithZIO[StorageMembersRepoInterface] {
      _.getAllStorageMembers(storageId)
    }

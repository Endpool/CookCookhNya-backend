package api

import api.ingredients.{
  createIngredientEndpoint,
  getIngredientEndpoint,
  deleteIngredientEndpoint,
  getAllIngredientsEndpoint,
}

import api.storages.{
  createStorageEndpoint,
  deleteStorageEndpoint,
  getStoragesEndpoint,
  getStorageMembersEndpoint,
  getStorageViewEndpoint,
}

import api.storages.ingredients.{
  addIngredientToStorageEndpoint,
  deleteIngredientFromStorageEndpoint,
  getStorageIngredientsEndpoint,
}

import sttp.tapir.ztapir.ZServerEndpoint

object AppEndpoints:
  val endpoints: List[ZServerEndpoint[AppEnv, Any]] =
    List(
      createIngredientEndpoint,
      getIngredientEndpoint,
      deleteIngredientEndpoint,
      getAllIngredientsEndpoint,
      addIngredientToStorageEndpoint,
      createStorageEndpoint,
      deleteIngredientFromStorageEndpoint,
      deleteStorageEndpoint,
      getStoragesEndpoint,
      getStorageIngredientsEndpoint,
      getStorageMembersEndpoint,
      getStorageViewEndpoint
    )

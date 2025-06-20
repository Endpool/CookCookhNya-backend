package api.endpoints

import api.endpoints.ingredient.{
  createIngredientEndpoint,
  getIngredientEndpoint,
  deleteIngredientEndpoint,
  getAllIngredientsEndpoint,
}

import api.endpoints.storage.{
  addIngredientToStorageEndpoint,
  createStorageEndpoint,
  deleteIngredientFromStorageEndpoint,
  deleteStorageEndpoint,
  getStoragesEndpoint,
  getStorageIngredientsEndpoint,
  getStorageMembersEndpoint,
  getStorageViewEndpoint
}

import sttp.tapir.ztapir.ZServerEndpoint

import api.AppEnv

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

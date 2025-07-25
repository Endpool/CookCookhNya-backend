package api

import api.ingredients.ingredientsEndpoints
import api.storages.storageEndpoints
import api.users.usersEndpoints
import api.recipes.recipeEndpoints
import api.shoppinglist.shoppingListEndpoints
import api.invitations.invitationEndpoints
import api.moderation.moderationEndpoints
import sttp.tapir.ztapir.ZServerEndpoint
import sttp.tapir.ztapir.RichZServerEndpoint

object AppEndpoints:
  val endpoints: List[ZServerEndpoint[AppEnv, Any]]
    =  storageEndpoints.map(_.widen)
    ++ ingredientsEndpoints.map(_.widen)
    ++ usersEndpoints.map(_.widen)
    ++ recipeEndpoints.map(_.widen)
    ++ shoppingListEndpoints.map(_.widen)
    ++ invitationEndpoints.map(_.widen)
    ++ moderationEndpoints.map(_.widen)
    :+ getPublicationRequestsHistory.widen

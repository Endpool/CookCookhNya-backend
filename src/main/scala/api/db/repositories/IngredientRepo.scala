package api.db.repositories

import api.db.tables.Ingredients
import api.domain.{Ingredient, IngredientId}
import com.augustnagro.magnum.magzio.*
import zio.{Task, ZLayer, RLayer}

trait IngredientRepoInterface:
  def add(ingredient: Ingredient): Task[Unit]
  def getById(id: IngredientId): Task[Option[Ingredient]]
  def removeById(id: IngredientId): Task[Unit]
  def getAll: Task[Vector[Ingredient]]

final case class IngredientRepo(xa: Transactor) extends Repo[Ingredient, Ingredients, IngredientId] with IngredientRepoInterface:
  override def add(ingredient: Ingredient): Task[Unit] =
    xa.transact(insert(ingredient))

  override def getById(id: IngredientId): Task[Option[Ingredient]] =
    xa.transact(findById(id).map(_.toDomain))

  override def removeById(id: IngredientId): Task[Unit] =
    xa.transact(deleteById(id))

  override def getAll: Task[Vector[Ingredient]] =
    xa.transact(findAll.map(_.toDomain))

object IngredientRepo:
  val layer: RLayer[Transactor, IngredientRepoInterface] =
    ZLayer.fromFunction(IngredientRepo(_))

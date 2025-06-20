package api.db.repositories

import api.db.tables.Ingredients
import api.db.tables.Ingredients.toDomain
import api.domain.Ingredient.CreationEntity
import api.domain.{Ingredient, IngredientId}
import api.domain.IngredientError.NotFound
import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait IngredientRepoInterface:
  def add(creationReq: CreationEntity): UIO[Ingredient]
  def getById(id: IngredientId): IO[NotFound, Ingredient]
  def removeById(id: IngredientId): IO[NotFound, Unit]
  def getAll: UIO[Vector[Ingredient]]

final case class IngredientRepo(xa: Transactor) extends Repo[CreationEntity, Ingredients, IngredientId] with IngredientRepoInterface:
  override def add(creationReq: CreationEntity): UIO[Ingredient] =
    xa.transact {
      val newIngredient: Ingredients = insertReturning(creationReq)
      toDomain(newIngredient)
    }.catchAll(_ => ZIO.succeed(null))

  override def getById(id: IngredientId): IO[NotFound, Ingredient] =
    val transaction: IO[NotFound, Option[Ingredients]] = xa.transact(findById(id)).catchAll {
      _ => ZIO.fail(NotFound(id))
    }
    
    transaction.flatMap {
      case Some(i) => ZIO.succeed(toDomain(i))
      case None    => ZIO.fail(NotFound(id))
    }
  
  override def removeById(id: IngredientId): IO[NotFound, Unit] =
    xa.transact(deleteById(id)).catchAll(_ => ZIO.unit)

  override def getAll: UIO[Vector[Ingredient]] =
    xa.transact(findAll.map(toDomain(_))).catchAll(_ => ZIO.succeed(Vector.empty))

object IngredientRepo:
  val layer: RLayer[Transactor, IngredientRepoInterface] =
    ZLayer.fromFunction(IngredientRepo(_))

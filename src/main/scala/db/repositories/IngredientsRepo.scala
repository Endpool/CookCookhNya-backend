package db.repositories

import db.tables.Ingredients
import db.tables.Ingredients.toDomain
import domain.{Ingredient, IngredientId}
import domain.IngredientError.NotFound

import com.augustnagro.magnum.magzio.*
import zio.{IO, RLayer, UIO, ZIO, ZLayer}

trait IIngredientsRepo:
  def add(name: String): UIO[Ingredient]
  def getById(id: IngredientId): IO[NotFound, Ingredient]
  def removeById(id: IngredientId): IO[NotFound, Unit]
  def getAll: UIO[Vector[Ingredient]]

private final case class IngredientCreationEntity(name: String)

final case class IngredientsRepo(xa: Transactor) extends Repo[IngredientCreationEntity, Ingredients, IngredientId] with IIngredientsRepo:
  override def add(name: String): UIO[Ingredient] =
    xa.transact {
      val newIngredient: Ingredients = insertReturning(IngredientCreationEntity(name))
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

object IngredientsRepo:
  val layer: RLayer[Transactor, IIngredientsRepo] =
    ZLayer.fromFunction(IngredientsRepo(_))

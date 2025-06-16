package backend

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredEncoder, ConfiguredDecoder, Configuration}

given Configuration = Configuration.default.withDiscriminator("type")
given Encoder[ErrorResponse] = ConfiguredEncoder.derived
given Decoder[ErrorResponse] = ConfiguredDecoder.derived

sealed trait ErrorResponse:
  def message: String

sealed trait IngredientError extends ErrorResponse
sealed trait StorageError extends ErrorResponse

final case class IngredientNotFound(id: IngredientId) extends IngredientError:
  override def message: String = s"Ingredient with id = $id is not found"

final case class StorageNotFound(id: StorageId) extends StorageError:
  override def message: String = s"Storage with id = $id is not found"

import zio.ZIO

val createIngredient: Ingredient => ZIO[Any, Nothing, Unit] = _ => ZIO.succeed(())

val getIngredient: IngredientId => ZIO[Any, Nothing, Option[Ingredient]] = _ => ZIO.succeed(None)

val getAllIngredients: ZIO[Any, Nothing, Option[List[Ingredient]]] = ZIO.succeed(None)

val deleteIngredient: IngredientId => ZIO[Any, Nothing, Unit] = _ => ZIO.succeed(())

val addIngredient: IngredientId => ZIO[Any, Nothing, Unit] = _ => ZIO.succeed(())

val deleteMyIngredient: IngredientId => ZIO[Any, Nothing, Unit] = _ => ZIO.succeed(())

val getAvailableIngredients: IngredientId => ZIO[Any, Nothing, Option[List[Ingredient]]] = _ => ZIO.succeed(None)

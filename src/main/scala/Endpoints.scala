import sttp.tapir.ztapir.*
import zio.ZIO

object Endpoints:
  val helloWorld: ZServerEndpoint[Any, Any] = endpoint
    .get
    .in("hello")
    .out(stringBody)
    .zServerLogic(_ => ZIO.succeed("Амирхан сосал"))

  val endpoints = List(helloWorld)

import sttp.tapir.ztapir.*
import zio.ZIO

object Endpoints:
  val helloWorld = endpoint
    .get
    .in("hello")
    .out(stringBody)
    .zServerLogic(_ => ZIO.succeed("Hello world!"))

  val endpoints = List(helloWorld)
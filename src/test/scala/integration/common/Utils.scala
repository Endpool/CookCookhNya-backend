package common

import domain.UserId

import io.circe.Encoder
import io.circe.parser.decode
import io.circe.syntax.*
import zio.http.{Request, Body, Header, MediaType}

object Utils:
  extension(req: Request)
    def addAuthorization(userId: UserId) =
      req.addHeader(Header.Authorization.Bearer(userId.toString))

    def withJsonBody[A](value: A)(using encoder: Encoder[A]) =
      req.addHeader(Header.ContentType(MediaType.application.json))
        .withBody(Body.fromCharSequence(value.asJson.toString))

  // redefining here for the sake of having default value of body
  def put(url: String, body: Body = Body.empty): Request = Request.put(url, body)
  def post(url: String, body: Body = Body.empty): Request = Request.post(url, body)

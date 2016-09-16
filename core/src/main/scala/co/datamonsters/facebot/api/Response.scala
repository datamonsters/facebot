package co.datamonsters.facebot.api

import pushka.annotation._
import pushka.{Ast, PushkaException, RW}

sealed trait Response

object Response {
  @pushka case class Success(
    @key("recipient_id") recipientId: String,
    @key("message_id") messageId: Option[String]
  ) extends Response

  @pushka case class Error(
    message: String,
    `type`: String,
    code: Int,
    @key("fbtrace_id") fbtraceId: String
  ) extends Response

  implicit val rw = new RW[Response] {
    def write(value: Response): Ast = value match {
      case success: Success => pushka.write(success)
      case error: Error => Ast("error" -> pushka.write(error))
    }
    def read(value: Ast): Response = value match {
      case Ast.Obj(m) if m.contains("error") => pushka.read[Error](m("error"))
      case obj: Ast.Obj => pushka.read[Success](obj)
      case _ => throw PushkaException(value, Response.getClass)
    }
  }
}

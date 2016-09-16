package co.datamonsters.facebot.api

import pushka.annotation._
import pushka.{Ast, PushkaException, RW}

sealed trait Messaging {
  def sender: Id
  def recipient: Id
}

object Messaging {

  // MessageReceivedCallback

  @pushka case class MessageReceived(
      sender: Id,
      recipient: Id,
      message: MessageReceived.Message
  ) extends Messaging

  object MessageReceived {

    @forceObject
    @pushka case class QuickReplay(payload: Ast)

    /**
      * Tip: match text and attachments as tuple
      * @example
      * ```scala
      * (msg.text, msg.attachments) {
      *    case (Some(text), _) => ???
      *    case (None, xs) => ???
      * }
      * ```
      */
    @pushka case class Message(
        text: Option[String],
        mid: String,
        seq: Int,
        @key("quick_reply") quickReply: Option[QuickReplay],
        @key("app_id") appId: Option[Long],
        @key("is_echo") isEcho: Boolean = false,
        metadata: String = "",
        attachments: Seq[Attachment] = Nil
    )
  }

  // PostbackReceivedCallback

  @pushka case class PostbackReceived(
      sender: Id,
      recipient: Id,
      postback: PostbackReceived.Payload
  ) extends Messaging

  object PostbackReceived {
    @pushka @forceObject case class Payload(payload: String)
  }

  // AuthenticationCallback

  @pushka case class Authentication(
      sender: Id,
      recipient: Id,
      ref: String
  ) extends Messaging

  // AccountLinkingCallback

  @pushka case class AccountLinking(
      sender: Id,
      recipient: Id,
      @key("account_linking") accountLinking: AccountLinking.StatusAndCode
  ) extends Messaging

  object AccountLinking {
    @pushka sealed trait AccountLinkingStatus

    object AccountLinkingStatus {
      case object Linked extends AccountLinkingStatus
      case object Unlinked extends AccountLinkingStatus
    }

    @pushka case class StatusAndCode(
        status: AccountLinkingStatus,
        @key("authorization_code") authorizationCode: Option[String]
    )
  }
  // MessageDeliveredCallback

  @pushka case class MessageDelivered(
      sender: Id,
      recipient: Id,
      delivery: MessageDelivered.Delivery
  ) extends Messaging

  object MessageDelivered {
    @pushka case class Delivery(mids: Seq[String], watermark: Long, seq: Int)
  }

  // MessageReadCallback

  @pushka case class MessageRead(
      sender: Id,
      recipient: Id,
      read: MessageRead.Read
  ) extends Messaging

  object MessageRead {
    @pushka case class Read(watermark: Long, seq: Int)
  }

  implicit val rw = new RW[Messaging] {
    def write(value: Messaging): Ast = value match {
      case callback: Messaging.AccountLinking => pushka.write(callback)
      case callback: Messaging.Authentication => pushka.write(callback)
      case callback: Messaging.MessageDelivered => pushka.write(callback)
      case callback: Messaging.MessageRead => pushka.write(callback)
      case callback: Messaging.MessageReceived => pushka.write(callback)
      case callback: Messaging.PostbackReceived => pushka.write(callback)
    }
    def read(value: Ast): Messaging = value match {
      case Ast.Obj(m) if m.contains("message") => pushka.read[MessageReceived](value)
      case Ast.Obj(m) if m.contains("postback") => pushka.read[PostbackReceived](value)
      case Ast.Obj(m) if m.contains("optin") => pushka.read[Authentication](value)
      case Ast.Obj(m) if m.contains("account_linking") => pushka.read[AccountLinking](value)
      case Ast.Obj(m) if m.contains("delivery") => pushka.read[MessageDelivered](value)
      case Ast.Obj(m) if m.contains("read") => pushka.read[MessageRead](value)
      case _ => throw PushkaException(value, Messaging.getClass)
    }
  }
}

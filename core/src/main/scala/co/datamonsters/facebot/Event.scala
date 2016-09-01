package co.datamonsters.facebot

import co.datamonsters.facebot.api.{Response, _}

import scala.concurrent.Future
import scala.language.higherKinds

class Event[+F[_]](val sendApi: SendApi[F],
                   val botId: String,
                   val entryId: String,
                   val entryTime: Long,
                   val messaging: Messaging)
    extends SendApi[F] {

  val reply: (Message, NotificationType) => F[Response] = sendMessage(messaging.sender, _, _)

  def sendMessage(recipient: Id,
                  message: Message,
                  notificationType: NotificationType = NotificationType.Regular): F[Response] =
    sendApi.sendMessage(recipient, message, notificationType)

  def typingOn(recipient: Id): F[Response] =
    sendApi.typingOn(recipient)

  def markSeen(recipient: Id): F[Response] =
    sendApi.markSeen(recipient)

  def typingOff(recipient: Id): F[Response] =
    sendApi.typingOff(recipient)
}

object Event {
  def unapply[F[_]](arg: Event[F]): Option[(String, Messaging)] =
    Some((arg.botId, arg.messaging))
}

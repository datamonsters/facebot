package co.datamonsters.facebot

import co.datamonsters.facebot.api.{Response, _}

import scala.concurrent.Future

class Event(val sendApi: SendApi,
  val botId: String,
  val entryId: String,
  val entryTime: Long,
  val messaging: Messaging) extends SendApi {

  def sendMessage(recipient: Id,
    message: Message,
    notificationType: NotificationType = NotificationType.Regular): Future[Response] =
    sendApi.sendMessage(recipient, message, notificationType)

  def typingOn(recipient: Id): Future[Response] =
    sendApi.typingOn(recipient)

  def markSeen(recipient: Id): Future[Response] =
    sendApi.markSeen(recipient)

  def typingOff(recipient: Id): Future[Response] =
    sendApi.typingOff(recipient)
}

object Event {
  def unapply(arg: Event): Option[(String, Messaging)] =
    Some((arg.botId, arg.messaging))
}

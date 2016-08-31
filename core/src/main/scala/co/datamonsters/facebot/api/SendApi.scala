package co.datamonsters.facebot.api

import scala.concurrent.Future

trait SendApi {

  def sendMessage(recipient: Id, message: Message, notificationType: NotificationType): Future[Response]

  def typingOn(recipient: Id): Future[Response]

  def typingOff(recipient: Id): Future[Response]

  def markSeen(recipient: Id): Future[Response]
}



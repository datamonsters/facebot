package co.datamonsters.facebot.api

import scala.language.higherKinds

trait SendApi[+F[_]] {

  def sendMessage(recipient: Id, message: Message, notificationType: NotificationType): F[Response]

  def typingOn(recipient: Id): F[Response]

  def typingOff(recipient: Id): F[Response]

  def markSeen(recipient: Id): F[Response]
}



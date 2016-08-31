package co.datamonsters

import co.datamonsters.facebot.api.SendApi

import scala.concurrent.Future

package object facebot {
  type ConnectionAdapter[T <: SendApi] = (Credentials, EventHandler) => T
  type EventHandler = PartialFunction[Event, Future[_]]
}

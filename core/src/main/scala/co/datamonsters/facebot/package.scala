package co.datamonsters

import co.datamonsters.facebot.api.SendApi

import scala.language.higherKinds

package object facebot {
  type ConnectionAdapter[F[_], T <: SendApi[F]] = (Credentials, EventHandler[F]) => T
  type EventHandler[F[_]] = PartialFunction[Event[F], F[_]]
}

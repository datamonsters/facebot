package co.datamonsters.facebot

import co.datamonsters.facebot.api.SendApi

import scala.concurrent.Future
import scala.language.higherKinds

/**
  * @author Aleksey Fomkin <aleksey.fomkin@gmail.com>
  */
object Facebot {

  def apply[F[_], T <: SendApi[F]]
    (adapter: ConnectionAdapter[F, T])
    (credentials: Credentials)
    (eventHandler: EventHandler[F]): T =
      adapter(credentials, eventHandler)
}

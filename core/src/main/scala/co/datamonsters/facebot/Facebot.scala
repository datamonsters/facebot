package co.datamonsters.facebot

import co.datamonsters.facebot.api.SendApi

import scala.concurrent.Future

/**
  * @author Aleksey Fomkin <aleksey.fomkin@gmail.com>
  */
object Facebot {

  def apply[T <: SendApi](adapter: ConnectionAdapter[T])(credentials: Credentials)(eventHandler: EventHandler): T = {
    adapter(credentials, eventHandler)
  }

//  def apply[T <: SendApi](adapter: ConnectionAdapter[T])(accessToken: String, verifyToken: String)(
//      eventHandler: EventHandler): T = {
//    adapter(Credentials(accessToken, verifyToken), eventHandler)
//  }
}

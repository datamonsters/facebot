package co.datamonsters.facebot.api

import pushka.annotation._

@pushka sealed trait Payload

object Payload {
  @forceObject case class Url(url: String) extends Payload
}

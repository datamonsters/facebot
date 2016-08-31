package co.datamonsters.facebot.api

import pushka.annotation.{key, pushka}

/**
  * @author Aleksey Fomkin <aleksey.fomkin@gmail.com>
  */
@pushka case class Request(
    recipient: Id,
    message: Option[Message],
    @key("sender_action") senderAction: Option[String],
    @key("notification_type") notificationType: Option[String]
)

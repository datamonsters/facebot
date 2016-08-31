package co.datamonsters.facebot.api

import pushka.annotation._

/**
  * https://developers.facebook.com/docs/messenger-platform/webhook-reference#format
  */
@pushka case class Webhook(
  `object`: String,
  entry: Seq[Entry]
)

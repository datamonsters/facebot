package co.datamonsters.facebot.api

import pushka.annotation._

@pushka case class Message(text: Option[String],
                           attachment: Option[Attachment],
                           @key("quick_replies") quickReplies: Option[Seq[Message.QuickReplay]]) {

  def withQuickReplays(quickReplies: Seq[Message.QuickReplay]): Message = {
    copy(quickReplies = Some(quickReplies))
  }
}

object Message {

  def apply(text: String): Message =
    Message(text = Some(text), None, None)
  def apply(attachment: Attachment): Message =
    Message(None, Some(attachment), None)

  def Image(uri: String): Message = Message(Attachment.Image(Attachment.Url(uri)))
  def Video(uri: String): Message = Message(Attachment.Video(Attachment.Url(uri)))
  def File(uri: String): Message = Message(Attachment.File(Attachment.Url(uri)))
  def Audio(uri: String): Message = Message(Attachment.Audio(Attachment.Url(uri)))

  @pushka case class QuickReplay(
      title: String,
      payload: String,
      @key("content_type") contentType: String = "text"
  )
}

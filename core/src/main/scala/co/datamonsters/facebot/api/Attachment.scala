package co.datamonsters.facebot.api

import pushka.{Ast, PushkaException, RW}
import pushka.annotation._

import scala.reflect.ClassTag

sealed trait Attachment {
  def `type`: String
}

object Attachment {

  implicit object AttachmentRw extends RW[Attachment] {

    def write(value: Attachment): Ast = value match {
      case a: Attachment.Image => pushka.write(a)
      case a: Attachment.Audio => pushka.write(a)
      case a: Attachment.Video => pushka.write(a)
      case a: Attachment.File => pushka.write(a)
      case a: Attachment.Template => pushka.write(a)
    }

    def read(value: Ast): Attachment = selectByField(value, "type") {
      case "image" => pushka.read[Image](value)
      case "audio" => pushka.read[Audio](value)
      case "video" => pushka.read[Video](value)
      case "file" => pushka.read[File](value)
      case "template" => pushka.read[Template](value)
    }
  }

  implicit object TemplatePayloadRw extends RW[TemplatePayload] {

    import TemplatePayload._

    def read(value: Ast): TemplatePayload = selectByField(value, "template_type") {
      case "button" => pushka.read[ButtonTemplate](value)
      case "generic" => pushka.read[GenericTemplate](value)
    }

    def write(value: TemplatePayload): Ast = value match {
      case x: ButtonTemplate => pushka.write(x)
      case x: GenericTemplate => pushka.write(x)
    }
  }

  implicit object ButtonRw extends RW[TemplatePayload.Button] {

    import TemplatePayload._
    import TemplatePayload.Button._

    def read(value: Ast): Button = selectByField(value, "type") {
      case "web_url" => pushka.read[WebUrl](value)
      case "postback" => pushka.read[Postback](value)
      case "phone_number" => pushka.read[PhoneNumber](value)
    }

    def write(value: Button): Ast = value match {
      case x: WebUrl => pushka.write(x)
      case x: Postback => pushka.write(x)
      case x: PhoneNumber => pushka.write(x)
    }
  }

  @pushka @forceObject case class Url(url: String)

  sealed trait TemplatePayload {
    def templateType: String
  }

  object TemplatePayload {

    @pushka case class ButtonTemplate(
        text: String,
        buttons: Seq[Button],
        @key("template_type") templateType: String = "button"
    ) extends TemplatePayload

    @pushka case class GenericTemplate(
        elements: Seq[GenericTemplate.Element],
        @key("template_type") templateType: String = "generic"
    ) extends TemplatePayload

    object GenericTemplate {

      def apply(elements: Element*): GenericTemplate = GenericTemplate(elements)

      @pushka case class Element(
          title: String,
          subtitle: String,
          buttons: Seq[Button],
          @key("image_url") imageUrl: String
      )
    }

    sealed trait Button

    object Button {
      /**
        * @param phoneNumber payload format mush be ‘+’ prefix followed by the country code, area code and local number
        */
      @pushka case class PhoneNumber(
          title: String,
          @key("payload") phoneNumber: String,
          `type`: String = "phone_number"
      ) extends Button

      @pushka case class WebUrl(title: String, url: String, `type`: String = "web_url") extends Button
      @pushka case class Postback(title: String, payload: String, `type`: String = "postback") extends Button
    }
  }

  def selectByField[T](value: Ast, field: String)(f: PartialFunction[String, T])(implicit ct: ClassTag[T]): T =
    value match {
      case Ast.Obj(m) if m.contains(field) =>
        m(field) match {
          case Ast.Str(x) =>
            f.lift(x) match {
              case Some(res) => res
              case None => throw PushkaException(value, ct.runtimeClass)
            }
          case _ => throw PushkaException(value, ct.runtimeClass)
        }
      case _ => throw PushkaException(value, ct.runtimeClass)
    }

  @pushka case class Template(payload: TemplatePayload, `type`: String = "template") extends Attachment
  @pushka case class Image(payload: Url, `type`: String = "image") extends Attachment
  @pushka case class Audio(payload: Url, `type`: String = "audio") extends Attachment
  @pushka case class Video(payload: Url, `type`: String = "video") extends Attachment
  @pushka case class File(payload: Url, `type`: String = "file") extends Attachment
}

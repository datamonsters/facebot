package co.datamonsters.facebot

import co.datamonsters.facebot.api._
import co.datamonsters.facebot.exceptions.{ApiIncompatibilityException, TokenValidationException}
import pushka.PushkaException
import pushka.json.printer
import pushka.json.parser

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Aleksey Fomkin <aleksey.fomkin@gmail.com>
  */
abstract class Connection(credentials: Credentials, eventHandler: EventHandler)(implicit ec: ExecutionContext) extends SendApi {

  val apiVersion = "v2.7"

  /**
    * Send request to platform
    *
    * @return response
    */
  def send(request: String): Future[String]

  /**
    * Take data from web hook
    *
    * @param method HTTP method
    * @param q Parsed query params from request line
    * @param body String body
    * @return Response to platform
    */
  def receive(botId: String,
              method: String,
              q: Map[String, Seq[String]],
              body: String): Future[String] = {
    try {
      method match {

        // Validation
        case "GET" if q.get("hub.mode") == Some(Seq("subscribe")) && q.contains("hub.challenge") =>
          q.get("hub.verify_token") match {
            case None => Future.failed(TokenValidationException("verify_token is not defined"))
            case Some(Seq(credentials.verifyToken)) => Future.successful(q("hub.challenge").head)
            case Some(xs) =>
              val message = s"verify_token is `${xs.mkString}` but `${credentials.verifyToken}` expected"
              throw TokenValidationException(message)
          }
        case "GET" =>
          val message = "Bad request. See https://developers.facebook.com/docs/graph-api/webhooks"
          throw TokenValidationException(message)

        // Callbacks
        case "POST" =>
          val webhook = pushka.json.read[Webhook](body)
          val events = webhook.entry flatMap { entry =>
            entry.messaging map { messaging =>
              new Event(this, botId, entry.id, entry.time, messaging)
            }
          }
          def loop(list: List[Event]): Future[String] = list match {
            case Nil => Future.successful("")
            case x :: xs => eventHandler.lift(x) match {
              case Some(future) => future.flatMap(_ => loop(xs))
              case None => loop(xs)
            }
          }
          loop(events.toList)
      }
    } catch {
      case e: PushkaException => Future(throw ApiIncompatibilityException(body, e))
      case e: Throwable => Future.failed(e)
    }
  }

  private def requestResponse(req: Request): Future[Response] = {
    val json = pushka.json.write(req)
    send(json).map(json => pushka.json.read[Response](json))
  }

  def sendMessage(recipient: Id, message: Message, notificationType: NotificationType): Future[Response] = {
    val req = Request(recipient, Some(message), None, Some(notificationType.value))
    requestResponse(req)
  }

  def typingOn(recipient: Id): Future[Response] = {
    val req = Request(recipient, None, Some("typing_on"), None)
    requestResponse(req)
  }

  def typingOff(recipient: Id): Future[Response] = {
    val req = Request(recipient, None, Some("typing_off"), None)
    requestResponse(req)
  }

  def markSeen(recipient: Id): Future[Response] = {
    val req = Request(recipient, None, Some("mark_seen"), None)
    requestResponse(req)
  }
}

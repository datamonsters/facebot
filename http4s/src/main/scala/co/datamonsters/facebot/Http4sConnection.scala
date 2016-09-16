package co.datamonsters.facebot

import co.datamonsters.facebot.api.{Response => FacebotResponse}
import co.datamonsters.facebot.exceptions.FacebotException
import org.http4s.Uri.{Authority, RegName}
import org.http4s._
import org.http4s.client.blaze._
import org.http4s.dsl._
import org.http4s.headers.`Content-Type`
import org.slf4j.LoggerFactory
import pushka.PushkaException

import scala.util.{Failure, Success, Try}
import scalaz.Scalaz._
import scalaz.concurrent.Task

final class Http4sConnection(credentials: Credentials, eventHandler: EventHandler[Task])
  extends Connection[Task](credentials, eventHandler) {

  private val logger = LoggerFactory.getLogger(Http4sConnection.getClass)

  private val client = PooledHttp1Client()

  private def facebotException(e: FacebotException) = {
    logger.debug("FacebotException", e)
    BadRequest(e.getMessage)
  }

  private def internalServerError(e: Throwable) = {
    logger.error("Error occurred when request processing", e)
    InternalServerError("Internal server error")
  }

  private def handleError[T](tried: Try[T]): Either[Task[Response], T] = tried match {
    case Failure(e: FacebotException) => Left(facebotException(e))
    case Failure(e) => Left(internalServerError(e))
    case Success(value) => Right(value)
  }

  val service = HttpService {
    case GET -> Root / _ :? params =>
      handleError(receiveVerify(params)) match {
        case Left(task) => task
        case Right(result) => Ok(result)
      }
    case request @ POST -> Root / botId =>
      request.bodyAsText.runFoldMap(identity) flatMap { body =>
        handleError(receive(botId, body)) match {
          case Right(tasks) =>
            val mappedTasks = tasks.map(_.map(_ => 0))
            Task.gatherUnordered(mappedTasks, exceptionCancels = true)
              .flatMap(_ => Ok(""))
              .handleWith {
                case e: FacebotException => facebotException(e)
                case e => internalServerError(e)
              }
          case Left(task) => task
        }
      }
  }

  def send(endpoint: String, request: String, parseResponse: String => FacebotResponse): Task[FacebotResponse] = {
    println(s">>> $request")
    val token = credentials.accessToken
    val fbUri = Uri(
      scheme = Some("https".ci),
      authority = Some(Authority(host = RegName("graph.facebook.com"))),
      path = s"/$apiVersion/me/$endpoint?access_token=$token"
    )
    val ct = `Content-Type`(MediaType.`application/json`, Charset.`UTF-8`)
    val req = Request(Method.POST, fbUri)
      .withBody(request)
      .withContentType(Some(ct))

    client(req) flatMap { response =>
      response.bodyAsText.runFoldMap(identity) map { body =>
        parseResponse(body)
      }
    }
  }
}


object Http4sConnection {
  def apply(credentials: Credentials)(eventHandler: EventHandler[Task]): Http4sConnection =
    new Http4sConnection(credentials, eventHandler)
}

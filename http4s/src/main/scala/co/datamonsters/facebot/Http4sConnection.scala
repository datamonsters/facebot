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

import scala.annotation.tailrec
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

  def send[R](endpoint: String,
              params: Map[String, String],
              bodyOpt: Option[String],
              parseResponse: String => R): Task[R] = {
    val fbUri = {

      @tailrec def paramsLoop(acc: List[String], list: List[(String, String)]): String = list match {
        case Nil => acc.reverse.mkString
        case (k, v) :: xs if acc.isEmpty => paramsLoop(s"?$k=$v" :: acc, xs)
        case (k, v) :: xs => paramsLoop(s"&$k=$v" :: acc, xs)
      }

      val paramsWithAccessToken = params + ("access_token" -> credentials.accessToken)
      val paramsPathPart = paramsLoop(Nil, paramsWithAccessToken.toList)

      Uri(
        scheme = Some("https".ci),
        authority = Some(Authority(host = RegName("graph.facebook.com"))),
        path = s"/$apiVersion/$endpoint/$paramsPathPart"
      )
    }


    val req = bodyOpt match {
      case None => client(Request(Method.GET, fbUri))
      case Some(body) =>
        val ct = `Content-Type`(MediaType.`application/json`, Charset.`UTF-8`)
        val req = Request(Method.POST, fbUri)
          .withBody(body)
          .withContentType(Some(ct))
        client(req)
    }

    req flatMap { response =>
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

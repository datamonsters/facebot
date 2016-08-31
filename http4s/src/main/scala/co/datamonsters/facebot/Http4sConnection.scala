package co.datamonsters.facebot

import java.nio.charset.StandardCharsets

import co.datamonsters.facebot.exceptions.FacebotException
import org.http4s.Uri.{Authority, RegName}
import org.http4s._
import org.http4s.client.blaze._
import org.http4s.dsl._
import org.http4s.headers.`Content-Type`
import org.slf4j.LoggerFactory

import scalaz.Scalaz._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scalaz.concurrent.Task

final class Http4sConnection(credentials: Credentials, eventHandler: EventHandler)
  (implicit ec: ExecutionContext) extends Connection(credentials, eventHandler) {

  import Http4sConnection._

  private val logger = LoggerFactory.getLogger(Http4sConnection.getClass)

  private val client = PooledHttp1Client()

  type Receivez = (String, String, Map[String, Seq[String]], String) => Task[Response]

  private val receivez: Receivez = receive(_, _, _, _)
    .asTask
    .flatMap {
      case response: String => Response(Ok).withBody(response)
      case _ => Ok()
    }
    .handleWith {
      case e: FacebotException =>
//        println("???????????????????????????")
//        println(e.getStackTrace.mkString("\n"))
        logger.debug("FacebotException", e)
        BadRequest(e.getMessage)
      case e =>
//        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!")
//        println(e.getStackTrace.mkString("\n"))
        logger.error("Error occurred when request processing", e)
        InternalServerError("Internal server error")
    }

  val service = HttpService {
    case GET -> Root / botId :? params =>
      receivez(botId, "GET", params, "")
    case request @ POST -> Root / botId =>
      request.bodyAsText.runFoldMap(identity) flatMap { body =>
        receivez(botId, "POST", Map.empty, body)
      }
  }

  def send(request: String): Future[String] = {
    println(s">>> $request")
    val token = credentials.accessToken
    val fbUri = Uri(
      scheme = Some("https".ci),
      authority = Some(Authority(host = RegName("graph.facebook.com"))),
      path = s"/$apiVersion/me/messages?access_token=$token"
    )
    val ct = `Content-Type`(MediaType.`application/json`, Charset.`UTF-8`)
    val req = Request(Method.POST, fbUri)
      .withBody(request)
      .withContentType(Some(ct))

    client.expect[String](req).runFuture()
  }
}

object Http4sConnection {

  def adapter(implicit ec: ExecutionContext): ConnectionAdapter[Http4sConnection] = {
    (credentials: Credentials, eventHandler: EventHandler) => {
      new Http4sConnection(credentials, eventHandler)
    }
  }

  implicit final class FutureExtensionOps[A](future: => Future[A]) {

    def asTask(implicit ec: ExecutionContext): Task[A] = {
      Task.async { register =>
        future onComplete {
          case scala.util.Success(v) => register(v.right)
          case scala.util.Failure(ex) => register(ex.left)
        }
      }
    }
  }

  implicit final class TaskExtensionOps[A](task: => Task[A]) {
    import scalaz.{-\/, \/-}
    def runFuture(): Future[A] = {
      val p: Promise[A] = Promise()
      task runAsync {
        case -\/(ex) => p.failure(ex)
        case \/-(r) => p.success(r)
      }
      p.future
    }
  }

}

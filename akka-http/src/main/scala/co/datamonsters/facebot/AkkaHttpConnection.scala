package co.datamonsters.facebot

import akka.actor.ActorSystem
import akka.http.javadsl.server.{PathMatchers, RouteResults}
import akka.http.scaladsl.{Http, server}
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import co.datamonsters.facebot.exceptions.FacebotException

import scala.io.StdIn
import akka.http.scaladsl.model.{ContentType, Uri}
import akka.http.scaladsl.model.headers.`Content-Type`
import co.datamonsters.facebot._
import co.datamonsters.facebot.api.Response

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import StatusCodes._
import akka.http.scaladsl.marshalling.Marshalling
import akka.http.scaladsl.server.Route

import scala.annotation.tailrec

/**
  * Created by z on 11.10.2016.
  */
final class AkkaHttpConnection(
    credentials: Credentials,
    eventHandler: EventHandler[Future])(implicit val system: ActorSystem)
    extends Connection[Future](credentials, eventHandler) {

  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  private def facebotException(e: FacebotException): HttpResponse = {
    HttpResponse(BadRequest, Nil, e.getMessage)
  }

  private def internalServerError(e: Throwable): HttpResponse = {
    HttpResponse(InternalServerError)
  }

  private def handleError[T](tried: Try[T]): Either[HttpResponse, T] =
    tried match {
      case Failure(e: FacebotException) => Left(facebotException(e))
      case Failure(e) => Left(internalServerError(e))
      case Success(value) => Right(value)
    }

  val facebookRestRoute = {
    path(Segment) { botID =>
      get {
        parameterMultiMap { params =>
          handleError[String](receiveVerify(params)) match {
            case Left(error) => complete(error)
            case Right(result) => complete(result)
          }
        }
      } ~
        post {
          entity(as[String]) { ent =>
            handleError(receive(botID, ent)) match {
              case Left(error) => complete(error)
              case Right(results) =>
                complete(results.map(a => a.map(c => 0)).mkString)
            }
          }
        }
    }
  }

  val poolClientFlow = Http().superPool[Int]()

  /**
    * Send request to platform
    *
    * @return response
    */
  override def send[R](endpoint: String,
                       params: Map[String, String],
                       bodyOpt: Option[String],
                       parseResponse: (String) => R): Future[R] = {

    val fbUri = {
      val paramsWithAccessToken = params + ("access_token" -> credentials.accessToken) // move it here cause you never use it else where
      val uri = Uri(
        scheme = "https",
        authority = Uri.Authority(host = Uri.Host("graph.facebook.com")),
        path = Uri.Path(s"/$apiVersion/$endpoint")
      )
      uri.withQuery(Uri.Query(paramsWithAccessToken))
    }

    val req = bodyOpt match {
      case None => HttpRequest(HttpMethods.GET, fbUri)
      case Some(body) =>
        val ct = ContentType(MediaTypes.`application/json`,
                             () => HttpCharsets.`UTF-8`)
        HttpRequest(HttpMethods.POST,
                    fbUri,
                    Nil,
                    HttpEntity(ct, ByteString(body)))
    }

    Source
      .single(req -> 0)
      .via(poolClientFlow)
      .map {
        case (Success(resp), _) if resp.status == StatusCodes.OK =>
          resp.entity.dataBytes
        case (Success(resp), _) =>
          println("Wrong Status " + resp.status); Source.empty[ByteString]
        case (Failure(fa), _) =>
          println("FAIL " + fa.toString); Source.empty[ByteString]
      }
      .runWith(Sink.head)
      .flatMap { a =>
        a.runFold(ByteString.empty)(_ ++ _)
          .map[R](a => parseResponse(a.utf8String))
      }
  }
}

object AkkaHttpConnection {
  def apply(credentials: Credentials)(eventHandler: EventHandler[Future])(
      implicit system: ActorSystem): AkkaHttpConnection =
    new AkkaHttpConnection(credentials, eventHandler)
}

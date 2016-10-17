# Facebot

This is a client for Facebook Messenger Platform API.

## Library Design

Connection layer has two implementions:
* [Http4s](https://github.com/http4s/http4s)
* [Akka HTTP](http://doc.akka.io/docs/akka/2.4.11/scala/http/index.html)

Connection works as a REST server to process Facebook Webhooks and a client to reply to bot with send method:
```scala
  def send[R](endpoint: String,
              params: Map[String, String],
              bodyOpt: Option[String],
              parseResponse: String => R): F[R]
```
As a serialization library it uses [Pushka](https://github.com/fomkin/pushka).

## Common Usage
In `facebot/akkahttp/src/main/scala/runDebug.scala` create `AkkaHttpConnection` object, provide it with `ActorSystem`, pass `Credentials` and `EventHandler` as parameters.
```scala
    implicit val system = ActorSystem("facebotActorSystem")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val facebotConnection = AkkaHttpConnection(Credentials(
      "ACCESS_TOKEN",
      "VERIFY_TOKEN")) {
      case sendApi @ Event(_, Messaging.MessageReceived(sender, _, message))
          if !message.isEcho =>
        sendApi.sendMessage(sender, Message("MESSAGE_TEXT"), Regular)
    }
```
Then initialize HTTP server and pass facebook REST route from `AkkaHttpConnection` object.
```scala
    val bindingFuture =
      Http().bindAndHandle(facebotConnection.facebookRestRoute, "localhost", 8701)
```
You can run it with `sbt 'project akkahttp' run`
```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import co.datamonsters.facebot._
import co.datamonsters.facebot.api.{Message, Messaging}
import co.datamonsters.facebot.api.NotificationType.Regular
import scala.io.StdIn

object runDebug {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("facebotActorSystem")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val facebotConnection = AkkaHttpConnection(Credentials(
      "ACCESS_TOKEN",
      "VERIFY_TOKEN")) {
      case sendApi @ Event(_, Messaging.MessageReceived(sender, _, message))
          if !message.isEcho =>
        sendApi.sendMessage(sender, Message("MESSAGE_TEXT"), Regular)
    }

    val bindingFuture =
      Http().bindAndHandle(facebotConnection.facebookRestRoute, "localhost", 8701)
    StdIn.readLine() // let it run until user presses return
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())

  }
}
```

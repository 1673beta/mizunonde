package bot.mizunonde.misskey

import org.apache.pekko.actor.typed.{ActorSystem, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.scaladsl.adapter.*

import scala.concurrent.duration.*
import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.*

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object BotCore {
  import DefaultJsonProtocol._
  object MisskeyJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val misskeyPostFormat: RootJsonFormat[MisskeyPost] = jsonFormat2(MisskeyPost)
  }
  import MisskeyJsonProtocol._
  case class MisskeyPost(text: String, i: String)

  sealed trait Command
  private case class PostCompleted(isSuccess: Boolean, message: String) extends Command
  case object SendPost extends Command

  def apply(apiUrl: String, apiKey: String, timezone: String = "Asia/Tokyo"): Behavior[Command] = Behaviors.setup { context =>
    implicit val system: ActorSystem[Nothing] = context.system
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    val zoneId = ZoneId.of(timezone)

    def postToMisskey(text: String): Unit = {
      val post = MisskeyPost(text, i = apiKey)
      val json = post.toJson
      val entity = HttpEntity(ContentTypes.`application/json`, post.toJson.compactPrint)

      val request = HttpRequest(
        method = HttpMethods.POST,
        uri = s"$apiUrl/notes/create",
        entity = entity
      )

      val responseFuture = Http().singleRequest(request)
      context.pipeToSelf(responseFuture) {
        case Success(response) if response.status.isSuccess() =>
          PostCompleted(isSuccess = true, message = "Post successful")
        case Success(response) if response.status.isFailure() =>
          PostCompleted(isSuccess = false, message = s"Post failed: ${response.status}")
        case Failure(exception) =>
          PostCompleted(isSuccess = false, message = s"Request failed: ${exception.getMessage}")
      }
    }

    context.system.scheduler.scheduleAtFixedRate(
      initialDelay = 1.minute,
      interval = 1.hours
    )(() => context.self ! SendPost)(context.executionContext)

    Behaviors.receiveMessage {
      case SendPost =>
        val now = LocalDateTime.now(zoneId)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val timeStr = formatter.format(now)
        val message = s"$timeStr です。 お水飲みましたか?"

        context.log.info("Sending post at {}", timeStr)
        postToMisskey(message)
        Behaviors.same

      case PostCompleted(success, message) =>
        if (success) {
          context.log.info(message)
        } else {
          context.log.error(message)
        }
        Behaviors.same
    }
  }
}
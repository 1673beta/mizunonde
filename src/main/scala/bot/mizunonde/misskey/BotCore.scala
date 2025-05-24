package bot.mizunonde.misskey

import org.apache.pekko.actor.typed.{ActorSystem, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.scaladsl.adapter.*

import scala.concurrent.duration.*
import java.time.{LocalDateTime, ZoneId, Duration}
import java.time.format.DateTimeFormatter
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.*

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object BotCore {
  import DefaultJsonProtocol._
  object MisskeyJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val misskeyPostFormat: RootJsonFormat[MisskeyPost] = jsonFormat2(MisskeyPost)
  }
  import MisskeyJsonProtocol._
  case class MisskeyPost(text: String, i: String)

  sealed trait Command
  private case class PostCompleted(isSuccess: Boolean, message: String, responseJson: Option[String] = None) extends Command
  case object SendPost extends Command

  def apply(apiUrl: String, apiKey: String, timezone: String = "Asia/Tokyo"): Behavior[Command] = Behaviors.setup { context =>
    implicit val system: ActorSystem[Nothing] = context.system
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    val zoneId = ZoneId.of(timezone)

    def calcInitialDelay(): FiniteDuration = {
      val now = LocalDateTime.now(zoneId)
      val nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
      val delay = java.time.Duration.between(now, nextHour)
      FiniteDuration(delay.toMillis, MILLISECONDS)
    }

    def postToMisskey(text: String): Unit = {
      val post = MisskeyPost(text, i = apiKey)
      val json = post.toJson
      val entity = HttpEntity(ContentTypes.`application/json`, post.toJson.compactPrint)

      context.log.info("送信するJSON: {}", post.toJson.prettyPrint)

      val request = HttpRequest(
        method = HttpMethods.POST,
        uri = s"$apiUrl/notes/create",
        entity = entity
      )

      val responseFuture = Http().singleRequest(request)

      // レスポンスの処理を拡張
      context.pipeToSelf(responseFuture.flatMap { response =>
        response.status match {
          case status if status.isSuccess() =>
            response.entity.toStrict(5.seconds).map { strictEntity =>
              val responseBody = strictEntity.data.utf8String
              (true, status.toString, Some(responseBody))
            }
          case status =>
            response.entity.toStrict(5.seconds).map { strictEntity =>
              val responseBody = strictEntity.data.utf8String
              (false, s"Post failed: $status", Some(responseBody))
            }
        }
      }) {
        case Success((isSuccess, message, responseBody)) =>
          PostCompleted(isSuccess = isSuccess, message = message, responseJson = responseBody)
        case Failure(exception) =>
          PostCompleted(isSuccess = false, message = s"リクエスト失敗: ${exception.getMessage}")
      }
    }

    val initialDelay = calcInitialDelay()

    context.system.scheduler.scheduleAtFixedRate(
      initialDelay = initialDelay,
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

      case PostCompleted(success, message, responseJson) =>
        if (success) {
          context.log.info(message)
          responseJson.foreach(json => context.log.info("レスポンスJSON: {}", json))
        } else {
          context.log.error(message)
          responseJson.foreach(json => context.log.error("エラーレスポンス: {}", json))
        }
        Behaviors.same
    }
  }
}
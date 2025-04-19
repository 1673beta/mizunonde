package bot.mizunonde.misskey

import org.apache.pekko.actor.typed.ActorSystem

object Main extends App {
  val config = ConfigLoader.loadConfig()

  if (config.apiKey.isEmpty) {
    println("Error: Environment variable MISSKEY_API_KEY is not set.")
    sys.exit(1)
  }

  println(s"API URL: ${config.apiUrl}")
  println(s"API Key: ${config.apiKey}")

  val system = ActorSystem(BotCore(config.apiUrl, config.apiKey), "MisskeyWaterReminder")

  println("Bot started.")

  system ! BotCore.SendPost

  Thread.currentThread().join()
}
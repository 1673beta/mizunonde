package bot.mizunonde.misskey

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Properties
import scala.jdk.CollectionConverters.*
import pureconfig.*

case class MisskeyConfig(apiUrl: String, apiKey: String, tz: String) derives ConfigReader

object ConfigLoader {
  def loadEnv(): Unit = {
    val envFile = new File(".env")
    if (envFile.exists()) {
      try {
        val lines = Files.readAllLines(Paths.get(".env")).asScala
        lines.foreach { line =>
          val parts = line.split("=", 2)
          if (parts.length == 2) {
            val key = parts(0).trim
            val value = parts(1).trim
            if (!sys.env.contains(key)) {
              System.setProperty(key, value)
            }
          }
        }
      } catch {
        case e: Exception => println(s"Error loading .env file: ${e.getMessage}")
      }

    }
  }

  def loadConfig(): MisskeyConfig = {
    loadEnv()

    val config = ConfigSource.default.load[MisskeyConfig] match {
      case Right(config) => config
      case Left(error) =>
        val apiUrl = sys.env.getOrElse("MISSKEY_API_URL", System.getProperty("MISSKEY_API_URL", "http://localhost:3000"))
        val apiKey = sys.env.getOrElse("MISSKEY_API_KEY", System.getProperty("MISSKEY_API_KEY", ""))
        val tz = sys.env.getOrElse("TZ", System.getProperty("TZ", "Asia/Tokyo"))
        MisskeyConfig(apiUrl, apiKey, tz)
    }
    config
  }
}

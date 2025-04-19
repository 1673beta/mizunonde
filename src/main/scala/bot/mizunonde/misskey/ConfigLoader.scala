package bot.mizunonde.misskey

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Properties
import scala.jdk.CollectionConverters.*
import pureconfig.*

case class MisskeyConfig(apiUrl: String, apiKey: String) derives ConfigReader

object ConfigLoader {
  def loadEnv(): Unit = {
    val envFile = new File(".env")
    if (envFile.exists()) {
      val properties = new Properties()
      properties.load(Files.newBufferedReader(Paths.get(".env")))

      properties.asScala.foreach { case (key, value) =>
        if (!sys.env.contains(key))
          System.setProperty(key, value)
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
        MisskeyConfig(apiUrl, apiKey)
    }
    config
  }
}

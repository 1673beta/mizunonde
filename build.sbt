ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"

enablePlugins(JlinkPlugin)

lazy val root = (project in file("."))
  .settings(
    name := "mizunonde",
    jlinkIgnoreMissingDependency := JlinkIgnore.everything,
    jlinkOptions += "--compress=2",
    jlinkModules ++= Seq(
      "java.base",
      "java.logging",
      "java.naming",
      "java.net.http",
      "java.security.jgss",
      "java.security.sasl",
      "jdk.crypto.ec",
      "jdk.security.auth"
    )
  )

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % "1.1.3",
  "org.apache.pekko" %% "pekko-stream" % "1.1.3",
  "org.apache.pekko" %% "pekko-slf4j" % "1.1.3",
  "org.apache.pekko" %% "pekko-http" % "1.1.0",
  "org.apache.pekko" %% "pekko-http-spray-json" % "1.1.0",
  "ch.qos.logback" % "logback-classic" % "1.5.18",
  "com.github.pureconfig" %% "pureconfig-core" % "0.17.8",
)
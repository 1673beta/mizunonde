ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"

lazy val root = (project in file("."))
  .settings(
    name := "mizunonde"
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
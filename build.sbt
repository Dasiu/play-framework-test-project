name := """play-scala-slick-compile-time-di-example-project"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  filters,
  evolutions,
  ws,
  ehcache,
  cacheApi,
  "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided",
  "com.typesafe.play" %% "play-json" % "2.6.2",
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0",
  "com.h2database" % "h2" % "1.4.187",
  "mysql" % "mysql-connector-java" % "5.1.36",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % "test",
  "com.nulab-inc" %% "scala-oauth2-core" % "1.3.0",
  "com.nulab-inc" %% "play2-oauth2-provider" % "1.3.0",
  "be.objectify" %% "deadbolt-scala" % "2.6.0",
  "org.mindrot" % "jbcrypt" % "0.4",
  "com.pauldijou" %% "jwt-play" % "0.14.0",
  "org.julienrf" %% "play-json-derived-codecs" % "4.0.0"
)

libraryDependencies += "org.pac4j" % "play-pac4j" % "4.0.0-RC1"
libraryDependencies += "org.pac4j" % "pac4j-jwt" % "2.1.0"
libraryDependencies += "org.pac4j" % "pac4j-http" % "2.1.0"

fork in run := true
organization  := "nat.traversal"

name          := "nat-traversal"

version       := "0.2"

scalaVersion  := "2.11.1"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/",
  "spray nightly repo" at "http://nightlies.spray.io"
)

val versions = Map[String, String](
  "akka" -> "2.3.3",
  "grizzled" -> "1.0.2",
  "logback" -> "1.1.2",
  "scala-parser-combinators" -> "1.0.1",
  "scala-xml" -> "1.0.2",
  "slf4j" -> "1.7.7",
  "specs2" -> "2.3.12",
  "spray" -> "1.3.1"
)

libraryDependencies ++= Seq(
  "org.slf4j"           %   "slf4j-api"     % versions("slf4j"),
  "org.clapper"         %% "grizzled-slf4j" % versions("grizzled"),
  "ch.qos.logback"      %   "logback-classic" % versions("logback"),
  "org.scala-lang.modules" %% "scala-parser-combinators" % versions("scala-parser-combinators"),
  "org.scala-lang.modules" %% "scala-xml"   % versions("scala-xml"),
  "io.spray"            %%  "spray-can"     % versions("spray"),
  "io.spray"            %%  "spray-client"  % versions("spray"),
  "io.spray"            %%  "spray-routing" % versions("spray"),
  "io.spray"            %%  "spray-testkit" % versions("spray") % "test",
  "com.typesafe.akka"   %%  "akka-actor"    % versions("akka"),
  "com.typesafe.akka"   %%  "akka-slf4j"    % versions("akka"),
  "com.typesafe.akka"   %%  "akka-testkit"  % versions("akka") % "test",
  "org.specs2"          %%  "specs2"        % versions("specs2") % "test"
)

seq(Revolver.settings: _*)

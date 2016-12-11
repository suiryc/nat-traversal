import sbt._
import Keys._

lazy val versions = Map[String, String](
  "akka"                     -> "2.4.14",
  "akka-http"                -> "10.0.0",
  "grizzled"                 -> "1.3.0",
  "java"                     -> "1.8",
  "logback"                  -> "1.1.8",
  "nat-traversal"            -> "0.3-SNAPSHOT",
  "scala"                    -> "2.11.8",
  "scala-parser-combinators" -> "1.0.4",
  "scala-xml"                -> "1.0.5",
  "scalatest"                -> "3.0.1",
  "slf4j"                    -> "1.7.21"
)


lazy val natTraversal = project.in(file(".")).
  settings(
    organization := "nat.traversal",
    name := "nat-traversal",
    version := versions("nat-traversal"),
    scalaVersion := versions("scala"),

    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-dead-code",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-unused",
      "-Ywarn-unused-import"
    ),
    scalacOptions in (Compile, doc) ++= Seq("-diagrams", "-implicits"),
    resolvers ++= Seq(
      Resolver.mavenLocal,
      "spray repo" at "http://repo.spray.io/",
      "spray nightly repo" at "http://nightlies.spray.io"
    ),

    libraryDependencies ++= Seq(
      "ch.qos.logback"         %  "logback-classic"          % versions("logback"),
      "com.typesafe.akka"      %% "akka-actor"               % versions("akka"),
      "com.typesafe.akka"      %% "akka-http"                % versions("akka-http"),
      "com.typesafe.akka"      %% "akka-http-core"           % versions("akka-http"),
      "com.typesafe.akka"      %% "akka-http-testkit"        % versions("akka-http"),
      "com.typesafe.akka"      %% "akka-http-xml"            % versions("akka-http"),
      "com.typesafe.akka"      %% "akka-slf4j"               % versions("akka"),
      "com.typesafe.akka"      %% "akka-testkit"             % versions("akka")                     % "test",
      "org.clapper"            %% "grizzled-slf4j"           % versions("grizzled"),
      "org.scala-lang.modules" %% "scala-parser-combinators" % versions("scala-parser-combinators"),
      "org.scala-lang.modules" %% "scala-xml"                % versions("scala-xml"),
      "org.scalatest"          %% "scalatest"                % versions("scalatest")                % "test",
      "org.slf4j"              %  "slf4j-api"                % versions("slf4j")
    ),

    publishMavenStyle := true,
    publishTo := Some(Resolver.mavenLocal)
  )

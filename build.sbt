import sbt._
import Keys._

lazy val versions = Map[String, String](
  "akka"                     -> "2.4.1",
  "grizzled"                 -> "1.0.2",
  "java"                     -> "1.8",
  "logback"                  -> "1.1.3",
  "nat-traversal"            -> "0.3-SNAPSHOT",
  "scala"                    -> "2.11.7",
  "scala-parser-combinators" -> "1.0.4",
  "scala-xml"                -> "1.0.5",
  "slf4j"                    -> "1.7.13",
  "specs2"                   -> "2.3.13",
  "spray"                    -> "1.3.1"
)


lazy val natTraversal = project.in(file(".")).
  settings(
    organization := "nat.traversal",
    name := "nat-traversal",
    version := versions("nat-traversal"),
    scalaVersion := versions("scala"),

    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-optimize",
      "-unchecked",
      "-Yinline-warnings"
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
      "com.typesafe.akka"      %% "akka-slf4j"               % versions("akka"),
      "com.typesafe.akka"      %% "akka-testkit"             % versions("akka")                     % "test",
      "io.spray"               %% "spray-can"                % versions("spray"),
      "io.spray"               %% "spray-client"             % versions("spray"),
      "io.spray"               %% "spray-routing"            % versions("spray"),
      "io.spray"               %% "spray-testkit"            % versions("spray")                    % "test",
      "org.clapper"            %% "grizzled-slf4j"           % versions("grizzled"),
      "org.scala-lang.modules" %% "scala-parser-combinators" % versions("scala-parser-combinators"),
      "org.scala-lang.modules" %% "scala-xml"                % versions("scala-xml"),
      "org.slf4j"              %  "slf4j-api"                % versions("slf4j"),
      "org.specs2"             %% "specs2"                   % versions("specs2")                   % "test"
    ),

    publishMavenStyle := true,
    publishTo := Some(Resolver.mavenLocal)
  )

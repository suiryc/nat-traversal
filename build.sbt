organization  := "nat.traversal"

name          := "nat-traversal"

version       := "0.1"

scalaVersion  := "2.10.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/",
  "spray nightly repo" at "http://nightlies.spray.io"
)

libraryDependencies ++= Seq(
  "com.typesafe"        %%  "scalalogging-slf4j" % "1.0.1",
  "org.slf4j"           %   "slf4j-api"     % "1.7.5",
  "ch.qos.logback"      %   "logback-classic" % "1.0.13",
  "io.spray"            %   "spray-can"     % "1.2-20130801",
  "io.spray"            %   "spray-client"  % "1.2-20130801",
  "io.spray"            %   "spray-routing" % "1.2-20130801",
  "io.spray"            %   "spray-testkit" % "1.2-20130801" % "test",
  "com.typesafe.akka"   %%  "akka-actor"    % "2.2.0",
  "com.typesafe.akka"   %%  "akka-slf4j"    % "2.2.0",
  "com.typesafe.akka"   %%  "akka-testkit"  % "2.2.0" % "test",
  "org.specs2"          %%  "specs2"        % "1.14" % "test"
)

seq(Revolver.settings: _*)

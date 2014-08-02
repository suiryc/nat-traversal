organization  := "nat.traversal"

name          := "nat-traversal"

version       := "0.3-SNAPSHOT"

scalaVersion  := "2.11.2"

scalacOptions ++= Seq("-deprecation", "-feature", "-optimize", "-unchecked", "-Yinline-warnings")

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/",
  "spray nightly repo" at "http://nightlies.spray.io"
)

val versions = Map[String, String](
  "akka" -> "2.3.4",
  "grizzled" -> "1.0.2",
  "java" -> "1.8",
  "logback" -> "1.1.2",
  "scala-parser-combinators" -> "1.0.2",
  "scala-xml" -> "1.0.2",
  "slf4j" -> "1.7.7",
  "specs2" -> "2.3.13",
  "spray" -> "1.3.1",
  "maven-compiler-plugin" -> "3.1",
  "maven-surefire-plugin" -> "2.17",
  "scala-maven-plugin" -> "3.1.6"
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


pomExtra := (
  <properties>
    <encoding>UTF-8</encoding>
  </properties>
  <build>
    <sourceDirectory>src/main/scala</sourceDirectory>
    <testSourceDirectory>src/test/scala</testSourceDirectory>
    <plugins>
      <plugin>
        <groupId>net.alchim31.maven</groupId>
        <artifactId>scala-maven-plugin</artifactId>
        <version>{ versions("scala-maven-plugin") }</version>
        <configuration>
          <args>
            <arg>-deprecation</arg>
            <arg>-feature</arg>
            <arg>-Yinline-warnings</arg>
            <arg>-optimize</arg>
            <arg>-unchecked</arg>
          </args>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>compile</goal>
              <goal>testCompile</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>{ versions("maven-compiler-plugin") }</version>
        <configuration>
          <source>{ versions("java") }</source>
          <target>{ versions("java") }</target>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>{ versions("maven-surefire-plugin") }</version>
        <configuration>
          <includes>
            <include>**/*Suite.class</include>
          </includes>
        </configuration>
      </plugin>
    </plugins>
  </build>
)


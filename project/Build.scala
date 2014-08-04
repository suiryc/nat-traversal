import sbt._
import Keys._


object NatTraversalBuild extends Build {

  lazy val base = file(".").getCanonicalFile

  lazy val copyPom = TaskKey[Unit]("copy-pom")

  val copyPomTask = copyPom <<= (makePom, streams) map { (pom, s) =>
    val dest = base / "pom.xml"
    s.log.info(s"Copy pom: $dest")
    IO.copyFile(pom, dest)
  }

  val extCompile = compile <<= (compile in Compile) dependsOn(copyPom)

  lazy val root = Project(
    id = "nat-traversal",
    base = base,
    settings = Defaults.defaultSettings ++ Seq(copyPomTask, extCompile)
  )
}


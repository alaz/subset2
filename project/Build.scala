import sbt._
import Keys._

object SubsetBuild extends Build {
  val ScalaVersionRE = """(\d+)\.(\d+).*""".r

  lazy val root = Project("subset", file("."))
}

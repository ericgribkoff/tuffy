import sbt._
import Keys._

object Publish {
  val nexus = s"http://utility.allenai.org:8081/nexus/content/repositories/"

  lazy val settings = Seq(
    publishTo <<= version { (v: String) =>
      if(v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "snapshots")
      else
        Some("releases" at nexus + "releases")
    })
}


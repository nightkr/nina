import sbt._
import Keys._

object NinaBuild extends Build {
	lazy val ninaSettings = Seq(
		resolvers += "Nullable.se" at "http://nexus.nullable.se/nexus/content/groups/public/",
		organization := "se.nullable.nina",
		version := "0.0.3-SNAPSHOT",
		scalaVersion := "2.9.1",

		publishMavenStyle := true,
		publishTo <<= (version) { version: String =>
			val nexus = "http://nexus.nullable.se/nexus/content/repositories/"
			if (version.trim.endsWith("-SNAPSHOT")) Some("snapshots" at nexus + "snapshots/") 
			else									Some("releases"  at nexus + "releases/")
		}
	)
	override lazy val settings = super.settings ++ ninaSettings

	lazy val all = Project(id = "nina-all", base = file(".")) aggregate(main, play)
	lazy val main = Project(id = "nina", base = file("nina"))
	lazy val play = Project(id = "nina-play", base = file("nina-play")) dependsOn(main)
}
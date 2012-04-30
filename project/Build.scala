import sbt._
import Keys._

object NinaBuild extends Build {
	lazy val ninaSettings = Seq(
		resolvers += "Nullable.se" at "http://nexus.nullable.se/nexus/content/groups/public/",
		organization := "se.nullable.nina",
		version := "0.0.1-SNAPSHOT"
	)
	override lazy val settings = super.settings ++ ninaSettings

	lazy val all = Project(id = "nina-all", base = file(".")) aggregate(main, play)
	lazy val main = Project(id = "nina", base = file("nina"))
	lazy val play = Project(id = "nina-play", base = file("nina-play")) dependsOn(main)
}
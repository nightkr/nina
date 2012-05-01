NINA
====
NINA is a simple database querying library/API for Scala. There is a provided implementation for MySQL through ANORM/Play2, but the NINA core has no external dependencies.

Installation (when used with Play 2)
------------------------------------
First add the Nullable.se Maven repository (http://nexus.nullable.se/nexus/content/repositories/releases/) to the Play project settings, then add a dependency on "se.nullable.nina" %% "nina-play" % "0.0.2". From a vanilla Build.scala looking like this

```scala
import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "mysite"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here
    )

}
```

you should modify it so it looks like this

```scala
import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "mysite"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "se.nullable.nina" %% "nina-play" % "0.0.2"
      // Add your project dependencies here,
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += "Nullable.se" at "http://nexus.nullable.se/nexus/content/repositories/releases/"
      // Add your own project settings here
    )

}
```

Usage
-----
First of all, in your model file, import NINA, as well as the implementation like this (assuming you use the play2 mysql implementation):

```scala
import nina._
import nina.impl.play.mysql._
```

Then you could declare your tables like this:

```scala
object MyTable extends Table("mytable") with PrimaryKey[Int] {
	val pk = id // Declare the primary key

    val id: Column[Int] = col[Int]("id") // Declare an INTEGER column with the name id
	val optional: Column[Option[String]] = optCol[String]("optional") // Declare some kind of nullable string column with the name option
}
```

Then you should `import nina._` in your consuming source files.


### Querying data
After that you could, for example, println a tuple of (id, optional) for each row:

```scala
for (id & optional <- MyTable get (MyTable.id & MyTable.optional) all()) {
	println(id -> optional)
}
```

Or if you, say, wanted to retrieve the optional for a given id:

```scala
def getOptionalFromId(id: Int): Option[String] = {
	MyTable where (MyTable.id === id) get MyTable.optional single() getOrElse None // single() returns a None if no row matched.
}
```

### Inserting data

```scala
MyTable insert (MyTable.id := 5, MyTable.optional := Some("hi"))
```

### Updating data

```scala
MyTable where (MyTable.id === 5) set (MyTable.optional := Some("bye"))
```

### Removing data

```scala
MyTable where (MyTable.id === 5) delete()
```
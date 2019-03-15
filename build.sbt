name := """cbft"""

version := "1.0"

scalaVersion := "2.11.7"

// Change this to another test framework if you prefer
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.4.11"

// Uncomment to use Akka
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.11"
//libraryDependencies += "com.typesafe.akka" %% "akka-dispatch" % "2.3.11"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.11" % "test"
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.30"

libraryDependencies += "redis.clients" % "jedis" % "2.9.0"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.3"

libraryDependencies ++= Seq(
  "com.roundeights" %% "hasher" % "1.2.0",
  "org.scala-lang" % "scala-reflect" % "2.11.7",
  "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.4"
)

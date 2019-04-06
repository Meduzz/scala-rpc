import sbt.{Credentials, Path}

name := "scala-rpc"

version := "0.3.0"

scalaVersion := "2.12.6"

organization := "se.chimps.rpc"

credentials += Credentials(Path.userHome / ".ivy2" / ".rpc")

publishTo := Some("se.chimps.rpc" at "https://yamr.kodiak.se/maven")

publishArtifact in (Compile, packageDoc) := false

resolvers += "se.kodiak.tools" at "https://yamr.kodiak.se/maven"

libraryDependencies ++= Seq(
	"org.scalatest" %% "scalatest" % "3.0.0" % "test",
	"io.nats" % "jnats" % "2.0.0",
	"se.kodiak.tools" %% "nuts" % "20190405",
	"org.json4s" %% "json4s-native" % "3.6.0"
)
import sbt.{Credentials, Path}

name := "scala-rpc"

version := "0.2.0"

scalaVersion := "2.12.6"

organization := "se.chimps.rpc"

credentials += Credentials(Path.userHome / ".ivy2" / ".rpc")

publishTo := Some("se.chimps.rpc" at "https://yamr.kodiak.se/maven")

publishArtifact in (Compile, packageDoc) := false

libraryDependencies ++= Seq(
	"org.scalatest" %% "scalatest" % "3.0.0" % "test",
	"io.nats" % "jnats" % "2.0.0",
	"org.json4s" %% "json4s-native" % "3.6.0"
)
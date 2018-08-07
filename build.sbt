name := "scala-rpc"

version := "0.0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
	"org.scalatest" %% "scalatest" % "3.0.0" % "test",
	"io.nats" % "jnats" % "2.0.0",
	"org.json4s" %% "json4s-native" % "3.6.0"
)
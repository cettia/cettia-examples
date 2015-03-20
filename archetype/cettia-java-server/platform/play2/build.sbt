name := "cettia-example-platform-play2"

version := "0"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "io.cettia" % "cettia-server" % "1.0.0-Alpha1",
  "io.cettia.platform" % "cettia-platform-bridge-play2" % "1.0.0-Alpha1"
)

resolvers += (
    "Local Maven Repository" at "file:///" + Path.userHome.absolutePath + "/.m2/repository"
)
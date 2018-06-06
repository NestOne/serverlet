
name := "demo"
version := "0.2.0"
scalaVersion := "2.11.8"
organization := "com.azavea"
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

resolvers += "locationtech" at "https://repo.locationtech.org/content/repositories/releases/"
scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Yinline-warnings",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:existentials",
  "-feature")
publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }

shellPrompt := { s => Project.extract(s).currentProject.id + " > " }

// We need to bump up the memory for some of the examples working with the landsat image.
javaOptions += "-Xmx4G"

fork in run := true
outputStrategy in run := Some(StdoutOutput)
connectInput in run := true

libraryDependencies ++= Seq(
  "org.locationtech.geotrellis" %% "geotrellis-spark" % "2.0.0-M3",
  "org.apache.spark" %% "spark-core" % "2.2.1",
  "com.typesafe.akka" %% "akka-actor"  % "2.4.3",
  "com.typesafe.akka" %% "akka-http" % "10.0.3",  
)



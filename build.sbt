scalaVersion in ThisBuild := "2.11.8"

// Play server
lazy val server = project.settings(
  scalaJSProjects := Seq(client),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  compile in Compile <<= (compile in Compile) dependsOn scalaJSPipeline,
  resolvers += Resolver.url("cake", url("https://dl.bintray.com/cakesolutions/maven")),
  sources in (Compile,doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false,
  libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % "1.0.0",
    "net.debasishg" %% "redisclient" % "3.2",
    "org.apache.kafka" % "kafka-clients" % "0.10.0.1"
  )
).enablePlugins(PlayScala)
  .dependsOn(sharedJvm)

// ScalaJS Frontend!
lazy val client = project.enablePlugins(
  ScalaJSPlugin, ScalaJSWeb).settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.6.0",
      "org.scala-js" %%% "scalajs-dom" % "0.9.0"
    )
  ).dependsOn(sharedJs)

// Protos defined in a shared project
lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(
    PB.protoSources in Compile := Seq(file("shared/src/main/protobuf")),
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    libraryDependencies ++= Seq(
      "com.trueaccord.scalapb" %%% "scalapb-runtime" % "0.5.43",
      "com.trueaccord.scalapb" %%% "scalapb-runtime" % "0.5.43" % "protobuf"
    ),
    sources in (Compile,doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false
  ).jsConfigure(_.enablePlugins(ScalaJSWeb))

lazy val sharedJvm = shared.jvm

lazy val sharedJs = shared.js

lazy val spark = project.settings(
  libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-streaming" % "2.0.1",
    "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.0.1",
    "net.debasishg" %% "redisclient" % "3.2"
  )
).dependsOn(sharedJvm)

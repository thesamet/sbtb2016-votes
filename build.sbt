scalaVersion in ThisBuild := "2.11.8"

lazy val server = project.settings(
  scalaJSProjects := Seq(client),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  compile in Compile <<= (compile in Compile) dependsOn scalaJSPipeline,
  libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % "1.0.0"
  )
).enablePlugins(PlayScala)

lazy val client = project.enablePlugins(ScalaJSPlugin, ScalaJSWeb)

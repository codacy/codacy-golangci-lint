val scalaVersionNumber = "2.13.18"
val circeVersion = "0.12.3"
val graalVersion = "22.3.3"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(GraalVMNativeImagePlugin)
  .settings(
    name := "codacy-golangci-lint",
    organization := "com.codacy",
    organizationName := "codacy",
    scalaVersion := scalaVersionNumber,
    test in assembly := {},
    Compile / mainClass := Some("com.codacy.golangcilint.GolangCILint"),
    libraryDependencies ++= Seq(
      "com.codacy" %% "codacy-analysis-cli-model" % "5.2.1",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.github.scopt" %% "scopt" % "4.1.0",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    ),
    graalVMNativeImageGraalVersion := Some(graalVersion),
    graalVMNativeImageOptions ++= Seq(
      "-O1",
      "-H:+ReportExceptionStackTraces",
      "--no-fallback",
      "--report-unsupported-elements-at-runtime",
      "--static"
    )
  )

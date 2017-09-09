inThisBuild(commonSettings)

initialCommands := """|import com.github.rockjam.telegram.bots._
                      |""".stripMargin

lazy val root =
  project
    .in(file("."))
    .settings(libraryDependencies ++= Dependencies.root)
    .settings(noPublish)
    .dependsOn(`tg-akka-http`, `tg-circe`, codegen, `tg-core`, `tg-json4s`, `tg-play-json`)
    .aggregate(`tg-akka-http`, `tg-circe`, codegen, `tg-core`, `tg-json4s`, `tg-play-json`)

lazy val `tg-akka-http` =
  project
    .settings(libraryDependencies ++= Dependencies.tgAkkaHttp)
    .dependsOn(`tg-core`)

lazy val `tg-circe` =
  project
    .settings(libraryDependencies ++= Dependencies.tgCirce)
    .dependsOn(`tg-core`)

lazy val codegen =
  project
    .settings(libraryDependencies ++= Dependencies.codegen)
    .settings(noPublish)

lazy val `tg-core` = project

//lazy val models =
//  project
//    .settings(libraryDependencies ++= Dependencies.models)
//    .enablePlugins(AutomateHeaderPlugin)

lazy val `tg-json4s` =
  project
    .settings(libraryDependencies ++= Dependencies.tgJson4s)
    .dependsOn(`tg-core`)

lazy val `tg-play-json` =
  project
    .settings(libraryDependencies ++= Dependencies.tgPlayJson)
    .dependsOn(`tg-core`)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

addCommandAlias("compileAll", ";subproject/run; root/compile")

// TODO: add this to run codegeneration before compilation
//    .settings(
//      compile in Compile := {
//        (run in (codegen, Compile)).toTask("").value
//        (compile in Compile).value
//      }
//    )

lazy val commonSettings = Seq(
  version := "0.0.2-SNAPSHOT",
  // Core settings
  organization := "com.github.rockjam",
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
  scalaVersion := Version.Scala,
  crossScalaVersions := Vector(scalaVersion.value),
  scalacOptions ++= Vector(
    "-unchecked",
    "-deprecation",
    "-language:_",
    "-target:jvm-1.8",
    "-encoding",
    "UTF-8"
  ),
  scalafmtVersion := "1.1.0",
  scalafmtOnCompile := true
)

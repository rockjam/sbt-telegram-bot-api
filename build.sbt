lazy val root =
  project
    .in(file("."))
    .settings(libraryDependencies ++= Dependencies.root)
    .settings(noPublish)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)
    .dependsOn(`tg-akka-http`, `tg-circe`, codegen, `tg-core`, `tg-json4s`, `tg-play-json`)
    .aggregate(`tg-akka-http`, `tg-circe`, codegen, `tg-core`, `tg-json4s`, `tg-play-json`)

lazy val `tg-akka-http` =
  project
    .settings(libraryDependencies ++= Dependencies.tgAkkaHttp)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)
    .dependsOn(`tg-core`)

lazy val `tg-circe` =
  project
    .settings(libraryDependencies ++= Dependencies.tgCirce)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)
    .dependsOn(`tg-core`)

lazy val codegen =
  project
    .settings(libraryDependencies ++= Dependencies.codegen)
    .settings(noPublish)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)

lazy val `tg-core` =
  project
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)

//lazy val models =
//  project
//    .settings(libraryDependencies ++= Dependencies.models)
//    .enablePlugins(AutomateHeaderPlugin, GitVersioning)

lazy val `tg-json4s` =
  project
    .settings(libraryDependencies ++= Dependencies.tgJson4s)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)
    .dependsOn(`tg-core`)

lazy val `tg-play-json` =
  project
    .settings(libraryDependencies ++= Dependencies.tgPlayJson)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)
    .dependsOn(`tg-core`)

initialCommands := """|import com.github.rockjam.telegram.bots._
                      |""".stripMargin

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

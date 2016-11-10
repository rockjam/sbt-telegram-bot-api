lazy val root =
  project
    .in(file("."))
    .settings(libraryDependencies ++= Dependencies.root)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)
    .dependsOn(`circe-kit`, codegen, models, `json4s-kit`)
    .aggregate(`circe-kit`, codegen, models, `json4s-kit`)

lazy val `circe-kit` =
  project
    .settings(libraryDependencies ++= Dependencies.circeKit)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)
    .dependsOn(models)

lazy val codegen =
  project
    .settings(libraryDependencies ++= Dependencies.codegen)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)

lazy val models =
  project
    .settings(libraryDependencies ++= Dependencies.models)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)

lazy val `json4s-kit` =
  project
    .settings(libraryDependencies ++= Dependencies.json4sKit)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)
    .dependsOn(models)

initialCommands := """|import com.github.rockjam.telegram.bots._
                      |""".stripMargin

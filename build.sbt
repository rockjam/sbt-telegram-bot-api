lazy val `telegram-bots` =
  project
    .in(file("."))
    .settings(libraryDependencies ++= Dependencies.telegramBots)
    .enablePlugins(AutomateHeaderPlugin, GitVersioning)

initialCommands := """|import com.github.rockjam.telegram.bots._
                      |""".stripMargin

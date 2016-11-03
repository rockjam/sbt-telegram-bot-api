lazy val `telegram-bots` =
  project.in(file(".")).enablePlugins(AutomateHeaderPlugin, GitVersioning)

libraryDependencies ++= Vector(
  Library.scalaTest % "test"
)

initialCommands := """|import com.github.rockjam.telegram.bots._
                      |""".stripMargin

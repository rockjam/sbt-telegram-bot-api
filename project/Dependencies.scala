import sbt._

object Version {
  final val Jsoup     = "1.10.1"
  final val Scala     = "2.11.8"
  final val Scalameta = "1.2.0"
  final val ScalaTest = "3.0.0"
}

object Library {
  val jsoup     = "org.jsoup"     %  "jsoup"     % Version.Jsoup
  val scalameta = "org.scalameta" %% "scalameta" % Version.Scalameta
  val scalaTest = "org.scalatest" %% "scalatest" % Version.ScalaTest
}

object Dependencies {
  import Library._
  val telegramBots = Vector(
    jsoup,
    scalameta,
    scalaTest % "test"
  )
}

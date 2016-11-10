import sbt._

object Version {
  final val Circe     = "0.6.0"
  final val Json4s    = "3.5.0"
  final val Jsoup     = "1.10.1"
  final val Scala     = "2.11.8"
  final val Scalameta = "1.2.0"
  final val ScalaTest = "3.0.0"
}

object Library {
  val circeCore    = "io.circe"       %% "circe-core"     % Version.Circe
  val circeGeneric = "io.circe"       %% "circe-generic"  % Version.Circe
  val circeParser  = "io.circe"       %% "circe-parser"   % Version.Circe
  val jsoup        = "org.jsoup"      %  "jsoup"          % Version.Jsoup
  val scalameta    = "org.scalameta"  %% "scalameta"      % Version.Scalameta
  val scalaReflect = "org.scala-lang" % "scala-reflect"   % Version.Scala
  val scalaTest    = "org.scalatest"  %% "scalatest"      % Version.ScalaTest
  val json4s       = "org.json4s"     %% "json4s-jackson" % Version.Json4s
  val json4sExt    = "org.json4s"     %% "json4s-ext"     % Version.Json4s
}

object Dependencies {
  import Library._

  val circeKit = Vector(
    circeCore,
    circeGeneric,
    circeParser
  )

  val codegen = Vector(
    jsoup,
    scalameta
  )

  val json4sKit = Vector(
    json4s,
    json4sExt
  )

  val models = Vector(scalaReflect) // TODO: remove

  val root = Vector(
    scalaReflect, // TODO: remove
    scalaTest % "test"
  )
}

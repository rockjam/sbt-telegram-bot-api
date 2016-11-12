import sbt._

object Version {
  final val Circe         = "0.6.0"
  final val Json4s        = "3.5.0"
  final val Jsoup         = "1.10.1"
  final val PlayJson      = "2.5.9"
  final val PlaJsonNaming = "1.1.0"
  final val Scala         = "2.11.8"
  final val Scalameta     = "1.2.0"
  final val ScalaTest     = "3.0.0"
}

object Library {
  val circeCore          = "io.circe"             %% "circe-core"           % Version.Circe
  val circeGeneric       = "io.circe"             %% "circe-generic"        % Version.Circe
  val circeGenericExtras = "io.circe"             %% "circe-generic-extras" % Version.Circe
  val circeParser        = "io.circe"             %% "circe-parser"         % Version.Circe
  val jsoup              = "org.jsoup"            %  "jsoup"                % Version.Jsoup
  val playJson           = "com.typesafe.play"    %% "play-json"            % Version.PlayJson
  val playJsonNaming     = "com.github.tototoshi" %% "play-json-naming"     % Version.PlaJsonNaming // temporary solution for snake case naming
  val scalameta          = "org.scalameta"        %% "scalameta"            % Version.Scalameta
  val scalaReflect       = "org.scala-lang"       % "scala-reflect"         % Version.Scala
  val scalaTest          = "org.scalatest"        %% "scalatest"            % Version.ScalaTest
  val json4s             = "org.json4s"           %% "json4s-jackson"       % Version.Json4s
  val json4sExt          = "org.json4s"           %% "json4s-ext"           % Version.Json4s
}

object Dependencies {
  import Library._

  val circeKit = Vector(
    circeCore,
    circeGeneric,
    circeGenericExtras,
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

  val playJsonKit = Vector(playJson, playJsonNaming)

  val root = Vector(
    scalaReflect, // TODO: remove
    scalaTest % "test"
  )
}

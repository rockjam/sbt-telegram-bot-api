/*
 * Copyright 2016 Nikolay Tatarinov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rockjam.telegram.bots

import java.nio.file.{ Files, Path, Paths }
import java.nio.file.StandardOpenOption._

object Main extends App {
  val schemaUrl      = "https://core.telegram.org/bots/api"
  val schema: Schema = HTMLSchemaParser.parse(schemaUrl)
  val modelsPackage  = "com.github.rockjam.telegram.bots.models"
  val circePackage   = "com.github.rockjam.telegram.bots.circe"

  val entitiesTrees: Map[String, String] =
    CodeGenerator.generate(modelsPackage, schema).mapValues(_.mkString("", "\n", "\n"))
  // should be scr_managed at compile time.
  writeFiles(
    basePath = Paths.get("models/src/main/scala/com/github/rockjam/telegram/bots/models"),
    toWrite = entitiesTrees
  )

  val circeTrees: Map[String, String] =
    CirceCodeGenerator
      .generate(schema, circePackage, modelsPackage)
      .mapValues(_.mkString("", "\n", "\n"))

  writeFiles(
    basePath = Paths.get("circe-kit/src/main/scala/com/github/rockjam/telegram/bots/circe"),
    toWrite = circeTrees
  )

  private def writeFiles(basePath: Path, toWrite: Map[String, String]): Unit = {
    Files.createDirectories(basePath)
    toWrite foreach {
      case (k, defn) ⇒
        Files.write(
          basePath.resolve(k),
          defn.getBytes,
          CREATE,
          TRUNCATE_EXISTING
        )
    }
  }

}

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

import java.nio.file.{ Files, Paths }
import java.nio.file.StandardOpenOption._

import scala.collection.immutable.Seq

object Main extends App {
  val schemaUrl      = "https://core.telegram.org/bots/api"
  val schema: Schema = HTMLSchemaParser.parse(schemaUrl)
  val trees: Map[String, Seq[String]] =
    CodeGenerator.generate("com.github.rockjam.telegram.bots.models", schema)
  // should be scr_managed at compile time.
  val basePath = Paths.get("models/src/main/scala/com/github/rockjam/telegram/bots/models")
  Files.createDirectories(basePath)
  trees foreach {
    case (k, defns) ⇒
      Files.write(
        basePath.resolve(k),
        defns.mkString("", "\n", "\n").getBytes,
        CREATE,
        TRUNCATE_EXISTING
      )
  }

}

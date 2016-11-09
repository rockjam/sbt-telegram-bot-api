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

import java.nio.file.StandardOpenOption.{ CREATE, TRUNCATE_EXISTING }
import java.nio.file.{ Files, Paths }

import scala.collection.immutable.Seq

object Main extends App {
  val schemaUrl      = "https://core.telegram.org/bots/api"
  val schema: Schema = HTMLSchemaParser.parse(schemaUrl)
  val trees: Map[String, Seq[String]] =
    CodeGenerator.generate("com.github.rockjam.telegram.bots.models", schema)

  val basePath = Paths.get("src/main/scala/com/github/rockjam/telegram/bots/models")
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
//
//  val allFields = schema.structs.flatMap(_.fields) ++ schema.methods.flatMap(_.fields)
//
//  val deep = allFields.groupBy(_.typ) collect {
//    case (typ, fields) if depth(typ) >= 3 ⇒
//      val name = fields
//        .map(_.name)
//        .distinct
//        .headOption
//        .map(n ⇒ StringUtils.camelize(n).capitalize)
//        .getOrElse(sys.error("Failed to get common name for derived"))
//      name → typ
//  }
//
//  def depth(t: ParsedType): Int = t match {
//    case OrType(_, b)   ⇒ 1 + depth(b)
//    case OptionType(tp) ⇒ depth(tp)
//    case _              ⇒ 0
//  }
//
}

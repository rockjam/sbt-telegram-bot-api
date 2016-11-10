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

  // regenerate schema only when something is passed as args
  if (args.nonEmpty) {
    val schemaUrl      = "https://core.telegram.org/bots/api"
    val schema: Schema = HTMLSchemaParser.parse(schemaUrl)
    val trees: Map[String, Seq[String]] =
      CodeGenerator.generate("com.github.rockjam.telegram.bots.models", schema)
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

//  import com.github.rockjam.telegram.bots.models._
//  import com.github.rockjam.telegram.bots.json4s._
//  import JsonHelpers._
//
//  val user = User(123L, "John", Some("Doe"), None)
//
//  val userThere = toJson(user)
//  val userBack  = fromJson[User](userThere)
//
//  println(s"User there: ${userThere}")
//  println(s"User back: ${userBack}")
//
//  val kick = KickChatMember(Left(21L), 21L)
//
//  val kickThere = toJson(kick)
//  val kickBack  = fromJson[KickChatMember](kickThere)
//
//  println(s"Kick there: ${kickThere}")
//  println(s"Kick back: ${kickBack}")
}

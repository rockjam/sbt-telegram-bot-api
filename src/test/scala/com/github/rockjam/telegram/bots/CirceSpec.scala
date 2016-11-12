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

import com.github.rockjam.telegram.bots.models._
import io.circe.Json

class CirceSpec extends SpecBase with SpecHelpers {
  behavior of "circe"

  it should "encode and decode requests" in one

  it should "encode either in untagged representation" in two

  // This import defines what we actually test.
  import com.github.rockjam.telegram.bots.circe._

  def one(): Unit = {
    import io.circe.syntax._

    val req  = GetFile("someFile")
    val resp = File("123", Some(123L), Some("path"))

    val jsonResp = Json
      .obj(
        "file_id"   → resp.fileId.asJson,
        "file_size" → resp.fileSize.asJson,
        "file_path" → resp.filePath.asJson
      )
      .asJson
      .noSpaces

    val fileResp = checkRequestResponse(req, jsonResp) { cursor ⇒
      cursor.downField("file_id").focus shouldEqual Some(req.fileId.asJson)
    }

    fileResp.result shouldEqual Some(resp)

    println(s"===resp is: ${fileResp}")

  }

  def two(): Unit = {
    import io.circe.syntax._

    val req  = GetChat(ChatId("@scala_ru"))
    val resp = Chat(1232L, "group", Some("scala ru"), None, None, None, None)
    val jsonResp = {

      Json
        .obj(
          "id"                          → resp.id.asJson,
          "type"                        → resp.`type`.asJson,
          "title"                       → resp.title.asJson,
          "username"                    → resp.username.asJson,
          "firstName"                   → resp.firstName.asJson,
          "lastName"                    → resp.lastName.asJson,
          "allMembersAreAdministrators" → resp.allMembersAreAdministrators.asJson
        )
        .asJson
        .noSpaces
    }

    val chatResp = checkRequestResponse(req, jsonResp) { cursor ⇒
      cursor.downField("chat_id").focus shouldEqual Some("@scala_ru".asJson)
    }

    chatResp.result shouldEqual Some(resp)

  }

}

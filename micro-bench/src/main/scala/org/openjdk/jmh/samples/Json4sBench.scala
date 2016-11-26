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

package org.openjdk.jmh.samples

import org.openjdk.jmh.annotations._

class Json4sBench extends JsonHelpers {

  import com.github.rockjam.telegram.bots.json4s._
  import com.github.rockjam.telegram.bots.models._

  @Benchmark
  def userToJson: String = {
    val user = User(
      12313L,
      "Name this",
      None,
      Some("helloworld")
    )
    toJson(user)
  }

  @Benchmark
  def userFromJson: User = {
    val json =
      """{"id":12313,"first_name":"Name this","username":"helloworld"}"""
    fromJson[User](json)
  }

  @Benchmark
  def inlineGameToJson: String = {
    val button1 = InlineKeyboardButton(
      "some text",
      Some("https://telegram.org"),
      None,
      None,
      None,
      None
    )
    val button2 = InlineKeyboardButton(
      "other text",
      None,
      None,
      Some("ehm, something else"),
      None,
      None
    )
    val inlineGame = InlineQueryResultGame(
      "game",
      "123",
      "blah blah",
      Some(
        InlineKeyboardMarkup(
          Seq(
            Seq(button1, button2, button1, button2),
            Seq(button2, button1, button2, button1),
            Seq(button2, button2, button2, button2),
            Seq(button2, button2, button2, button2)
          )
        )
      )
    )
    toJson(inlineGame)
  }

}

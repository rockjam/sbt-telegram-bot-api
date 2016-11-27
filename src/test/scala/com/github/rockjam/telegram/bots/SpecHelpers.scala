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

trait SpecHelpers { this: SpecBase ⇒ // todo: remove it
  import scala.reflect.runtime.universe._
  import JsonHelpers._

  // приходит request в виде кейс класса. мы его превращаем в json
  // нужно проверить этот json, что он соответствует какой-то строке.
  // строки можно делать через интерполяцию, через мапу, как-то еще

  // или же нужна нужна штука чтобы ходить по Json-у

  // затем мы отдаем в виде строки ответ. ответ можно сериализовать вручную
  // можно чтобы были заготовленные ответы, response чекается через тайптег
  // мы десериализуем эту строку в кейс класс, смотрим что получили.

  def checkRequestResponse(req: BotApiRequest, resp: String)(
      checkRequest: io.circe.HCursor ⇒ Unit)(
                            implicit e: Encode[BotApiRequest],
                            m: Manifest[req.Resp], // can we use typetag only? it would be better
                            d: Decode[BotApiResponse[req.Resp]],
                            tt: TypeTag[req.Resp]): BotApiResponse[req.Resp] = {
    val json = toJson(req)
    println(s"==== json is: ${json}")

    val jsonCursor = io.circe.parser
      .parse(json)
      .fold(
        pf ⇒ fail(s"Request is not valid JSON object: ${pf}"),
        identity
      )
      .hcursor
    checkRequest(jsonCursor)
    println(s"request json is: ${json}")

    println(s"=====tt 1: ${tt}")
    val response = fakeRequest(resp)
    println(s"response: ${response}")
    fromJson[BotApiResponse[req.Resp]](response)
  }

  def fakeRequest(resp: String): String = {
    def apiResponse(s: String) = s"""
                                    |{
                                    |  "ok": true,
                                    |  "result": $s,
                                    |  "description": "this is description"
                                    |}
    """.stripMargin
    apiResponse(resp)
  }

}

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

import scala.concurrent.{ ExecutionContext, Future }

trait TelegramRequests extends JsonHelpers {

  val botToken: String

  private val TelegramApiUri = s"https://api.telegram.org"

  def request(req: BotApiRequest)(
      implicit encode: Encode[BotApiRequest],
      manifest: Manifest[req.Resp],
      decode: Decode[BotApiResponse[req.Resp]],
      client: HttpClient,
      ec: ExecutionContext
  ): Future[BotApiResponse[req.Resp]] = {
    val jsonReq = toJson(req)
    val resp    = client.makeRequest(s"${TelegramApiUri}/bot${botToken}/${req.requestName}", jsonReq)
    resp map (r ⇒ fromJson[BotApiResponse[req.Resp]](r))
  }

}

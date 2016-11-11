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

package com.github.rockjam.telegram.bots.circe

import com.github.rockjam.telegram.bots.models._
import io.circe.generic.extras.Configuration
import io.circe.{ Decoder, Encoder, Json }

trait BotApiEncoders {
  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseKeys.withDefaults

  import io.circe.generic.extras.semiauto._
  import io.circe.syntax._

  implicit val getFileEncoder: Encoder[GetFile] = deriveEncoder

  implicit val getMeEncoder: Encoder[GetMe] = deriveEncoder

  implicit val getChatEncoder: Encoder[GetChat] = deriveEncoder

  implicit val fileDecoder: Decoder[File] = deriveDecoder

  implicit val chatDecoder: Decoder[Chat] = deriveDecoder

  implicit def apiResponseDecoder[T: Decoder]: Decoder[ApiResponse[T]] = deriveDecoder

  implicit val botApiRequestEncoder: Encoder[BotApiRequest] = new Encoder[BotApiRequest] {
    def apply(a: BotApiRequest): Json = a match {
      case r: GetFile ⇒ r.asJson
      case r: GetMe   ⇒ r.asJson
      case r: GetChat ⇒ r.asJson
    }
  }

}

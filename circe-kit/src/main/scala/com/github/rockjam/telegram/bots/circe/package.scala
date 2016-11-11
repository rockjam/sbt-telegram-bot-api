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

import com.github.rockjam.telegram.bots.models.{ Decode, Encode }
import io.circe.{ Decoder, Encoder }

package object circe extends CustomEncoders with BotApiEncoders {

  import io.circe.parser

  implicit def encode[T](implicit encoder: Encoder[T]): Encode[T] = new Encode[T] {
    def apply(v: T): String = encoder(v).noSpaces
  }

  implicit def decode[T](implicit decoder: Decoder[T]): Decode[T] = new Decode[T] {
    def apply(json: String)(implicit m: Manifest[T]): T =
      parser.decode[T](json) match {
        case Left(e)  ⇒ throw new RuntimeException(s"Failed to decode json: ${json}", e)
        case Right(t) ⇒ t
      }
  }

}

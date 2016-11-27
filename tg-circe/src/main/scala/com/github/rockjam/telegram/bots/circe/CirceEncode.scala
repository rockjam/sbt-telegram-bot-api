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

import com.github.rockjam.telegram.bots.{ Decode, Encode }
import io.circe.{ Decoder, Encoder, Printer }
import io.circe.generic.extras.Configuration

// This is constant, should not be generated.
trait CirceEncode {
  import io.circe.syntax._
  import io.circe.parser

  implicit lazy val derivationConfig: Configuration =
    Configuration.default.withSnakeCaseKeys.withDefaults

  private val dropNullKeys: Printer = Printer.noSpaces.copy(dropNullKeys = true)

  implicit def encode[T: Encoder]: Encode[T] = new Encode[T] {
    def apply(v: T): String = v.asJson.pretty(dropNullKeys)
  }

  implicit def decode[T: Decoder]: Decode[T] = new Decode[T] {
    def apply(json: String)(implicit m: Manifest[T]): T =
      parser.decode[T](json) match {
        case Left(e)  ⇒ throw new RuntimeException(s"Failed to decode json: ${json}", e)
        case Right(t) ⇒ t
      }
  }

}

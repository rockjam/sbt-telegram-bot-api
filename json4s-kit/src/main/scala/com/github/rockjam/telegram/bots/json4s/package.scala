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

import com.github.rockjam.telegram.bots.models.{ Decoder, Encoder }
import org.json4s._
import org.json4s.jackson.JsonMethods.{ compact, parse, render }

package object json4s extends Json4sFormats {

  implicit def encoder[T]: Encoder[T] = new Encoder[T] {
    def apply(v: T): String =
      compact(render(Extraction.decompose(v).snakizeKeys))
  }

  implicit def decoder[T]: Decoder[T] = new Decoder[T] {
    def apply(json: String)(implicit m: Manifest[T]): T =
      parse(json).camelizeKeys.extract[T](formats, manifest)

  }
}

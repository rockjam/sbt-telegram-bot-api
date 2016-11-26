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

import com.github.rockjam.telegram.bots.models.{ Decode, Encode }

import scala.reflect.Manifest

object JsonHelpers extends JsonHelpers

trait JsonHelpers {

  def toJson[T: Encode](v: T): String                = implicitly[Encode[T]].apply(v)
  def fromJson[T: Decode: Manifest](json: String): T = implicitly[Decode[T]].apply(json)

}

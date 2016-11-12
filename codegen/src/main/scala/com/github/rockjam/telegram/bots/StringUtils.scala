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

import java.util.Locale._

object StringUtils {
  def lowerize(word: String): String =
    word.headOption map (_.toLower + word.tail) getOrElse word

  // Taken from https://github.com/json4s/json4s project: org.json4s.MonadicJValue as is
  def camelize(word: String): String = {
    val w = pascalize(word)
    w.substring(0, 1).toLowerCase(ENGLISH) + w.substring(1)
  }

  private[this] def pascalize(word: String): String = {
    val lst = word.split("_").toList
    (lst.headOption.map(s ⇒ s.substring(0, 1).toUpperCase(ENGLISH) + s.substring(1)).get ::
      lst.tail.map(s ⇒ s.substring(0, 1).toUpperCase + s.substring(1))).mkString("")
  }

}

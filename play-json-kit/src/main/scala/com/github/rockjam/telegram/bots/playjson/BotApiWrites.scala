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

package com.github.rockjam.telegram.bots.playjson

trait BotApiWrites extends EitherFormats {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  import com.github.tototoshi.play.json.JsonNaming

  import com.github.rockjam.telegram.bots.models._

  private def emptyWrites[T] = Writes[T](_ ⇒ JsObject.apply(Seq.empty))

  implicit val getFileWrites: Writes[GetFile] = JsonNaming.snakecase(Json.writes[GetFile])

  implicit val getMeWrites: Writes[GetMe] = emptyWrites

  implicit val getChatWrites: Writes[GetChat] = JsonNaming.snakecase(Json.writes[GetChat])

  implicit val fileReads: Reads[File] = JsonNaming.snakecase(Json.reads[File])

  implicit val chatReads: Reads[Chat] = JsonNaming.snakecase(Json.reads[Chat])

  // WTF???
  implicit def apiResponseReads[T: Reads]: Reads[ApiResponse[T]] =
    JsonNaming.snakecase(
      (
        (JsPath \ "ok").read[Boolean] and
          (JsPath \ "result").readNullable[T] and
          (JsPath \ "description").readNullable[String] and
          (JsPath \ "errorCode").readNullable[Int]
      )(ApiResponse.apply(_, _: Option[T], _, _)))

  implicit val botApiRequestEncoder: Writes[BotApiRequest] = new Writes[BotApiRequest] {
    def writes(o: BotApiRequest): JsValue = o match {
      case r: GetFile ⇒ Json.toJson[GetFile](r)(getFileWrites)
      case r: GetMe   ⇒ Json.toJson[GetMe](r)(getMeWrites)
      case r: GetChat ⇒ Json.toJson[GetChat](r)(getChatWrites)
    }
  }

}

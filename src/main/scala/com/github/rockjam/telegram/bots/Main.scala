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

import java.nio.file.StandardOpenOption.{ CREATE, TRUNCATE_EXISTING }
import java.nio.file.{ Files, Paths }

import scala.collection.immutable.Seq
import scala.reflect.runtime.universe._

object Main extends App {

  import com.github.rockjam.telegram.bots.models._
  import com.github.rockjam.telegram.bots.json4s._
//  import com.github.rockjam.telegram.bots.circe._
//  import io.circe.generic.extras.auto._ // TODO: this should GONE after we genereate semiauto encoders
  import JsonHelpers._

  val user = User(123L, "John", Some("Doe"), None)

  val userThere = toJson(user)
  val userBack  = fromJson[User](userThere)

  println(s"User there: ${userThere}")
  println(s"User back: ${userBack}")

  val kick = KickChatMember(Right("hellowo"), 21L)

  val kickThere = toJson(kick)
  val kickBack  = fromJson[KickChatMember](kickThere)

  println(s"Kick there: ${kickThere}")
  println(s"Kick back: ${kickBack}")

  def serializeRequest(req: BotApiRequest) =
    toJson(req)
//  serializeRequest(
//    GetFile("somefile")
//  )

  val reply: ReplyMarkup = ForceReply(forceReply = true, None)

  val replyThere = toJson(reply)
  println(s"Reply there: ${replyThere}")

  val getFile = GetFile("somefile")

  val getFileThere = toJson(getFile)
  println(s"Get file there: ${getFileThere}")

  def reqResp(req: BotApiRequest)(implicit tt: TypeTag[req.Resp]): ApiResponse[req.Resp] = {
    val json = serializeRequest(req)
    println(s"json is: ${json}")

    println(s"=====tt 1: ${tt}")
    val resp = makeRequest[req.Resp](json)
    println(s"resp: ${resp}")
    resp
  }

  val fileResp = reqResp(GetFile("someFile"))
  println(s"=== jsoned :${toJson(fileResp)}")
  val getMeResp = reqResp(GetMe())

  def makeRequest[T: TypeTag](json: String) = {
    val tt = typeOf[T]
    println(s"=====tt 2: ${tt}")
    val result = tt match {
      case t if t =:= typeOf[File] ⇒
        File("sss", Some(22L), Some("qweqew"))
      case t if t =:= typeOf[User] ⇒
        User(1213L, "John", Some("Doe"), None)
    }

    ApiResponse(
      ok = true,
      result = Some(
        result.asInstanceOf[T]
      ),
      Some("this is description"),
      None
    )
  }

}

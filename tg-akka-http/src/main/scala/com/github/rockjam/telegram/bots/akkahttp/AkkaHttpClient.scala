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

package com.github.rockjam.telegram.bots.akkahttp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.HttpRequest
import akka.stream.{ ActorMaterializer, Materializer }
import com.github.rockjam.telegram.bots.HttpClient

import scala.concurrent.Future

object AkkaHttpClient {
  def apply()(implicit system: ActorSystem): AkkaHttpClient = new AkkaHttpClient(system)
}

class AkkaHttpClient(system: ActorSystem) extends HttpClient {
  private val http = Http(system)

  import system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()(system)

  def makeRequest(uri: String, body: String): Future[String] =
    for {
      resp <- http.singleRequest(
        HttpRequest(POST, uri, entity = body)
      )
      respBody <- resp.entity.dataBytes.runFold("")(_ ++ _.utf8String)
    } yield respBody

}

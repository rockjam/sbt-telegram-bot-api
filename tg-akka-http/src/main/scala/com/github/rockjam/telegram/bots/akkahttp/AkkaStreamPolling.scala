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

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Source
import akka.stream.stage.{ GraphStage, GraphStageLogic, OutHandler }
import com.github.rockjam.telegram.bots._
import com.github.rockjam.telegram.bots.models.{ GetUpdates, Update }

import scala.concurrent.duration._
import scala.concurrent._
import scala.util.{ Failure, Success, Try }

trait AkkaStreamPolling extends TelegramRequests {

  protected lazy val pollingInterval: FiniteDuration = 10.seconds

  def updatesSource(implicit encode: Encode[BotApiRequest],
                    decode: Decode[BotApiResponse[Seq[Update]]],
                    client: HttpClient,
                    system: ActorSystem): Source[Update, NotUsed] = {
    import system.dispatcher
    implicit val mat = ActorMaterializer()
    Source.fromGraph(new GraphStage[SourceShape[Update]] {
      val out: Outlet[Update]        = Outlet("UpdatesSource")
      def shape: SourceShape[Update] = SourceShape(out)

      def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
        new GraphStageLogic(shape) {
          private var buf: Vector[Update] = Vector.empty
          private var currentOffset       = 0L

          setHandler(out, new OutHandler {
            def onPull(): Unit =
              if (buf.isEmpty) {
                pollingRequest(currentOffset).onComplete(callback)
              } else {
                deliverBuf()
              }
          })

          private val callback: Try[BotApiResponse[Seq[Update]]] ⇒ Unit =
            getAsyncCallback[Try[BotApiResponse[Seq[Update]]]] {
              case Success(response) ⇒
                if (response.ok) {
                  response.result match {
                    case Some(updates) ⇒
                      buf ++= updates
                      updates.lastOption foreach { last ⇒
                        currentOffset = last.updateId + 1
                      }
                      deliverBuf()
                    case None ⇒
                      fail(
                        out,
                        new RuntimeException("Response is fine, but no result provided")
                      )
                  }
                } else {
                  fail(
                    out,
                    new RuntimeException(
                      s"Request failed with cause: ${response.description}, error code: ${response.errorCode}")
                  )
                }
              case Failure(err) ⇒
                fail(out, err) // fail, or failStage?
            }.invoke(_)

          private def deliverBuf(): Unit = {
            val head +: tail = buf
            buf = tail
            push(out, head)
          }

          private def pollingRequest(offset: Long): Future[BotApiResponse[Seq[Update]]] =
            request(
              GetUpdates(
                offset = Some(offset),
                timeout = Some(pollingInterval.toSeconds),
                limit = None,
                allowedUpdates = None
              ))
        }
    })

  }

}

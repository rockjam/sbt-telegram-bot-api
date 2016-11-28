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

import com.github.rockjam.telegram.bots.models.{ GetUpdates, Update }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

trait CallbackPolling extends TelegramRequests {
  type UpdateHandler = PartialFunction[Update, Future[Unit]]

  val botToken: String

  protected lazy val pollingInterval: FiniteDuration = 10.seconds

  private val InitialRequest = GetUpdates(
    offset = Some(0L),
    timeout = Some(pollingInterval.toSeconds),
    limit = None
  )

  private def loop(req: GetUpdates)(handleUpdate: UpdateHandler)(
      implicit encode: Encode[BotApiRequest],
      decode: Decode[BotApiResponse[Seq[Update]]],
      client: HttpClient,
      ec: ExecutionContext
  ): Future[Unit] =
    for {
      response <- request(req)
      _ = println(s"=== Updates ok: ${response.ok}")
      _ = println(s"=== Updates description: ${response.description}")
      _ = println(s"=== Updates errorCode: ${response.errorCode}")
      _ = println(s"=== Updates result: ${response.result}")

      _ = response.result match {
        case Some(us) ⇒ us foreach handleUpdate
        case None     ⇒ println("=== Got no updates!")
      }
      maxUpdateId = maxOffset(response.result.getOrElse(Seq.empty)) orElse req.offset // TODO: write it another way.
      _           = println(s"=== new maxUpdate id: ${maxUpdateId}")
      resume <- loop(req.copy(offset = maxUpdateId))(handleUpdate)
    } yield resume

  def startPolling(handleUpdate: UpdateHandler)(
      implicit encode: Encode[BotApiRequest],
      decode: Decode[BotApiResponse[Seq[Update]]],
      client: HttpClient,
      ec: ExecutionContext
  ): Unit =
    loop(InitialRequest)(handleUpdate) onComplete {
      case Success(_) ⇒ println("Successfully finished polling future")
      case Failure(err) ⇒
        println(s"Polling future failed with exception: ${err}")
        loop(InitialRequest)(handleUpdate)
        println("Restarted polling")
    }

  private def maxOffset(updates: Seq[Update]): Option[Long] =
    if (updates.isEmpty) {
      None
    } else {
      Some(updates.map(_.updateId).max + 1)
    }
}

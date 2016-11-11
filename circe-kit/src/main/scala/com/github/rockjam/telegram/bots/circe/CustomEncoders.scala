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

import io.circe.Decoder._
import io.circe.{ Decoder, Encoder, HCursor, Json }

trait CustomEncoders {

//  https://github.com/travisbrown/circe/issues/216#issuecomment-219290054

//  import io.circe._, io.circe.generic.auto._, io.circe.jawn._, io.circe.syntax._
//
//  implicit val encodeIntOrString: Encoder[Either[Int, String]] =
//    Encoder.instance(_.fold(_.asJson, _.asJson))
//
//  implicit val decodeIntOrString: Decoder[Either[Int, String]] =
//    Decoder[Int].map(Left(_)).or(Decoder[String].map(Right(_)))

  implicit def eitherEncoder[A, B](implicit ea: Encoder[A],
                                   eb: Encoder[B]): Encoder[Either[A, B]] =
    new Encoder[Either[A, B]] {
      def apply(a: Either[A, B]): Json = a.fold(l ⇒ ea(l), r ⇒ eb(r))
    }

  implicit def eitherDecoder[A, B](implicit da: Decoder[A],
                                   ab: Decoder[B]): Decoder[Either[A, B]] =
    new Decoder[Either[A, B]] {
      def apply(c: HCursor): Result[Either[A, B]] = {
        val tryA = c.as[A]
        tryA match {
          case Left(err) ⇒
            val tryB = c.as[B]
            tryB match {
              case Left(err)  ⇒ Left(err)
              case Right(res) ⇒ Right(Right(res))
            }
          case Right(res) ⇒ Right(Left(res))
        }
      }
    }
}

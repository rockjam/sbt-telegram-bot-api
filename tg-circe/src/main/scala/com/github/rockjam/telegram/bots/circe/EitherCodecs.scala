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

// This is constant, should not be generated.
//implementation taken from: https://github.com/travisbrown/circe/issues/216#issuecomment-219290054
// TODO: rename to codecs
trait EitherCodecs {

  import io.circe._, io.circe.syntax._

  implicit def eitherEncoder[A: Encoder, B: Encoder]: Encoder[Either[A, B]] =
    Encoder.instance(_.fold(_.asJson, _.asJson))

  implicit def eitherDecoder[A: Decoder, B: Decoder]: Decoder[Either[A, B]] =
    Decoder[A].map(Left.apply).or(Decoder[B].map(Right.apply))
}

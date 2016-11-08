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

import scala.collection.immutable.Seq

sealed trait ParsedType

/**
  * Represents literal type that can be mapped to scala's standard types.
  * Examples: Integer, Int, String, Boolean, True, Float, Float number
  *
  * @param name type name
  */
final case class LiteralType(name: String) extends ParsedType

/**
  * Represents Telegram bot API internal data structures:
  * Examples: User, Chat, Message, etc..
  *
  * @param name type name
  */
final case class StructType(name: String) extends ParsedType

/**
  * Represents optional values.
  * Parametrised with type parameter `tp`
  *
  * @param tp type parameter
  */
final case class OptionType(tp: ParsedType) extends ParsedType

/**
  * Represents sequence of values with same type `tp`
  *
  * @param tp type parameter
  */
final case class ListType(tp: ParsedType) extends ParsedType

/**
  * Represents a value of one of two possible types.
  * Parametrised with two type parameters: `a` and `b`
  *
  * @param a left type parameter
  * @param b right type parameter
  */
final case class OrType(a: ParsedType, b: ParsedType) extends ParsedType

//sealed trait AST

final case class Field(name: String, typ: ParsedType, description: String)
final case class Structure(name: String, fields: Seq[Field], baseType: Option[BaseType]) // TODO: possibly add description // TODO: baseType: is it String or StructureBase?
final case class BaseType(name: String)
final case class Method(name: String, returnTyp: ParsedType, fields: Seq[Field]) // TODO: possibly add description

// maybe enumeration too

final case class Schema(structs: Seq[Structure], bases: Seq[BaseType], methods: Seq[Method])

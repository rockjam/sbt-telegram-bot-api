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
final case class LiteralType(name: String)            extends ParsedType // Integer, String, Boolean and other True-s
final case class StructType(name: String)             extends ParsedType // Structures defined by Telegram API
final case class OptionType(tp: ParsedType)           extends ParsedType // Option parametrised with type parameter tp.
final case class ListType(tp: ParsedType)             extends ParsedType // List parametrised with type parameter tp.
final case class OrType(a: ParsedType, b: ParsedType) extends ParsedType // a or b

//sealed trait AST

final case class Field(name: String, typ: ParsedType, description: String)
final case class Structure(name: String, fields: Seq[Field], baseType: Option[String]) // TODO: possibly add description // TODO: baseType: is it String or StructureBase?
final case class StructureBase(name: String)
final case class Method(name: String, returnTyp: ParsedType, fields: Seq[Field]) // TODO: possibly add description

// should be BaseType too
// maybe enumeration too

final case class Schema(structs: Seq[Structure], bases: Seq[StructureBase], methods: Seq[Method])

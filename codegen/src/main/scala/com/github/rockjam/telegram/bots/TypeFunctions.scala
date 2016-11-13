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

import scala.annotation.tailrec
import scala.collection.immutable.Seq

trait TypeFunctions {

  /**
    * Recursively extract all structure names, that structures from `initialStructNames` depend on.
    *
    * @param initialStructNames names of structures we start from
    * @param allStructs all structures from Telegram bot API
    * @return extracted set of structure names
    */
  protected def extractStructNamesDeep(initialStructNames: Set[String],
                                       allStructs: Seq[Structure]): Set[String] = {
    val structTypeNames =
      allStructs.groupBy(_.name).mapValues { structs ⇒ // should be list of single structure
        (for {
          struct <- structs
          field  <- struct.fields
        } yield extractStructNames(field.typ)).flatten.toSet
      }

    // for structure names, collect structure types in their fields,
    // that we not yet found, and execute search for them.
    @tailrec def aux(structNames: Set[String], foundNames: Set[String]): Set[String] = {
      val unknownStructTypes = structNames flatMap { name ⇒
        structTypeNames.getOrElse(name, Set.empty) diff foundNames
      }
      if (unknownStructTypes.nonEmpty) aux(unknownStructTypes, foundNames ++ unknownStructTypes)
      else foundNames
    }

    aux(initialStructNames, initialStructNames)
  }

  /**
    * Extracts structure names from `ParsedType`
    *
    * @param typ type from schema
    * @return sequence of structure names
    */
  protected def extractStructNames(typ: ParsedType): Seq[String] = typ match {
    case StructType(name) ⇒ Seq(name)
    case OptionType(tp)   ⇒ extractStructNames(tp)
    case ListType(tp)     ⇒ extractStructNames(tp)
    case OrType(at, bt)   ⇒ extractStructNames(at) ++ extractStructNames(bt)
    case _                ⇒ Seq.empty
  }

}

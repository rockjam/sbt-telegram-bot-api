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
import scala.meta.Term.Param
import scala.meta._

object CodeGenerator {

  def generate(schema: Schema): Unit = {
    println(predefined)

    val traitTrees = schema.bases map structureBaseToCaseClass
    println("========================== Traits")
    traitTrees foreach println

    val structureTrees = schema.structs map structureToCaseClass
    // write those to file. possibly grouped by updates/messages/other shit
    println("========================== Structures")
    structureTrees foreach println

    val methodTrees = schema.methods map methodToCaseClass
    println("========================== Methods")
    methodTrees foreach println
  }

  private def structureBaseToCaseClass(structureBase: StructureBase): Defn.Trait = {
    val traitName = Type.Name(structureBase.name)
    q"sealed trait $traitName"
  }

  private def structureToCaseClass(structure: Structure): Defn.Class = {
    val templopt = structure.baseType map { t ⇒
      val name = Ctor.Name(t)
      template"$name"
    }
    val structName              = Type.Name(structure.name)
    val params: Seq[Term.Param] = toParams(structure.fields)

    templopt match {
      case Some(templ) ⇒ q"final case class $structName ( ..$params ) extends $templ"
      case None        ⇒ q"final case class $structName ( ..$params )"
    }
  }

  private def methodToCaseClass(method: Method): Defn.Class = {
    val methodName = Type.Name(method.name.capitalize)
    val params     = toParams(method.fields)
    val returnType = toScalaType(method.returnTyp)

    q"final case class $methodName ( ..$params ) extends BotApiRequest[$returnType]"
  }

  private def predefined: Seq[Tree] =
//    val alias = "type alias ChatId = Either[Long, String]"
    Seq(
      q"sealed trait BotApiRequest[Resp]"
    )

  // to case class fields
  private def toParams(fields: Seq[Field]): Seq[Param] = fields map {
    case Field(name, typ, _) ⇒
      val paramName = Term.Name(StringUtils.camelize(name))
      val paramType = toScalaType(typ)
      param"$paramName: $paramType"
  }

  private def toScalaType(parsedType: ParsedType): Type = {
//    val typeAlias: PartialFunction[ParsedType, Type] = {
//      case OrType(LiteralType("Integer" | "Int"), LiteralType("String")) => t"ChatId"
//    }

    val literalType: PartialFunction[String, Type] = {
      case "Integer" | "Int"  ⇒ t"Long"
      case "Boolean" | "True" ⇒ t"Boolean"
      case "String"           ⇒ t"String"
      case "Float"            ⇒ t"Float"
    }

    parsedType match {
      case LiteralType(name) ⇒ literalType(name)
      case StructType(name) ⇒
        val typ = Type.Name(name)
        t"$typ"
      case OptionType(tp) ⇒
        val scalaTp = toScalaType(tp)
        t"Option[$scalaTp]"
      case ListType(tp) ⇒
        val scalaTp = toScalaType(tp)
        t"Seq[$scalaTp]"
      case OrType(a, b) ⇒
        val aType = toScalaType(a)
        val bType = toScalaType(b)
        t"Either[$aType, $bType]"
    }
  }

}

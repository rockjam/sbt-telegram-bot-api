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
import scala.meta._

//Generates bot api entities
object CodeGenerator {

  /**
    * Generate entities source code from structured schema.
    *
    * Transformation rules:
    * • `schema.structs` become case classes
    * • `schema.baseTypes` become traits
    * • `schema.methods` become case classes that extends `BotApiRequest`
    *
    * Generated sources split in three files:
    * • package.scala: package object with common code(type aliases, etc.)
    * • structures.scala: traits definitions, definitions of structures and generated InputFile structure
    * • methods.scala: methods case classes definition
    *
    * @param basePackage base package name
    * @param schema Telegram bot API structured schema
    * @return mapping from file name to sequence of sources
    */
  def generate(basePackage: String, schema: Schema): Map[String, Seq[String]] = {
    val packageName = ScalametaCommon.packageDef(basePackage)
//    val packObject  = packageObject(basePackage)

    val traitTrees     = schema.baseTypes map baseTypeToTrait
    val structureTrees = schema.structs map structureToCaseClass

    val methodTrees = schema.methods map methodToCaseClass

    Map(
//      "package.scala" → packObject.map(_.syntax),
      "structures.scala" → (Seq(packageName, InputFile) ++ traitTrees ++ structureTrees)
        .map(_.syntax),
      "methods.scala" → (Seq(packageName, BotApiRequestImport) ++ methodTrees).map(_.syntax)
    )
  }

  /**
    * Produce sealed trait from base type definition
    *
    * @param base base type definition
    * @return sealed trait
    */
  private def baseTypeToTrait(base: BaseType): Defn.Trait = {
    val traitName = Type.Name(base.name)
    q"sealed trait $traitName"
  }

  /**
    * Produce case class from structure definition.
    * `struct.name` becomes class name,
    * `struct.fields` become class params,
    * if `struct.baseType` is present,
    * resulting case class will extend that base type
    *
    * @param struct structure definition
    * @return structure case class
    */
  private def structureToCaseClass(struct: Structure): Defn.Class = {
    val templopt = struct.baseType map { t ⇒
      val name = Ctor.Name(t.name)
      template"$name"
    }
    val structName              = Type.Name(struct.name)
    val params: Seq[Term.Param] = struct.fields map toParam

    templopt match {
      case Some(templ) ⇒ q"final case class $structName ( ..$params ) extends $templ"
      case None        ⇒ q"final case class $structName ( ..$params )"
    }
  }

  /**
    * Produce case class from method definition.
    * Capitalized `meth.name` becomes class name,
    * `meth.fields` become class params,
    * `meth.responseType` becomes type parameter of BotApiRequest
    *
    * @param meth method definition
    * @return method case class
    */
  private def methodToCaseClass(meth: Method): Defn.Class = {
    val methodName = Type.Name(meth.name.capitalize)
    val params     = meth.fields map toParam

    val stats: Seq[Stat] = {
      val requestName  = q"override def requestName: String = ${meth.name}"
      val responseType = q"type Resp = ${toScalaType(meth.responseType)}"
      Seq(requestName, responseType)
    }

    q"final case class $methodName ( ..$params ) extends BotApiRequest { ..$stats }"
  }

  private val InputFile: Defn.Class = q"final case class InputFile(fileId: String)"

  private val BotApiRequestImport: Import =
    q"import com.github.rockjam.telegram.bots.BotApiRequest"

//  // base type for all bot API methods
//  private val BotApiRequest: Defn.Trait = {
//    val stats: Seq[Stat] = {
//      val resp    = q"type Resp"
//      val reqName = q"def requestName: String"
//      Seq(resp, reqName)
//    }
//    q"sealed trait BotApiRequest { ..$stats }"
//  }

  /**
    * Produce package name for given package
    * It contains common code used across structures and methods.
    *
    * @param basePackage package name
    * @return package for object
    */
//  private def packageObject(basePackage: String) = {
//    val stats = Seq(
//      q"type ChatId = Either[Long, String]",
//      q"""final case class ApiResponse[Resp](
//        ok: Boolean,
//        result: Option[Resp],
//        description: Option[String],
//        errorCode: Option[Int])
//      """
//    )
//
//    val (optPackage, packObjName) = (basePackage split "\\.").toList match {
//      case init :+ last   ⇒ Some(ScalametaCommon.packageDef(init mkString ".")) → last
//      case List() :+ last ⇒ None                                                → last
//    }
//
//    val packObj = {
//      val name  = Term.Name(packObjName)
//      val templ = template"{ ..$stats } "
//      q"package object $name extends $templ"
//    }
//
//    optPackage.fold(Seq[Stat](packObj))(p ⇒ Seq(p, packObj))
//  }

  /**
    * Produces parameter that can be used in classes or methods.
    *
    * @param field field of method or structure
    * @return class/method parameter
    */
  private def toParam(field: Field): Term.Param = {
    val paramName = Term.Name(StringUtils.camelize(field.name))
    val paramType = toScalaType(field.typ)
    param"$paramName: $paramType"
  }

  /**
    * Converts `ParsedType` into scala.meta.Type.
    * Changes common types with alias.
    *
    * @param parsedType type from schema
    * @return scala.meta.Type
    */
  // TODO: move it to common
  def toScalaType(parsedType: ParsedType): Type = {
    def alias(pt: ParsedType): Option[Type] = pt match {
//      case OrType(LiteralType("Integer" | "Int"), LiteralType("String")) ⇒ Some(t"ChatId")
      case _ ⇒ None
    }

    val literalType: PartialFunction[String, Type] = {
      case "Integer" | "Int"  ⇒ t"Long"
      case "Boolean" | "True" ⇒ t"Boolean"
      case "String"           ⇒ t"String"
      case "Float"            ⇒ t"Float"
    }

    alias(parsedType) getOrElse {
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

}

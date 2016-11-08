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

object CodeGenerator {

  /**
    * Generate source code from strucutred schema.
    *
    * Transformation rules:
    * • `schema.structs` become case classes
    * • `schema.baseTypes` become traits
    * • `schema.methods` become case classes that extends `BotApiRequest[ResponseType]`
    *
    * Generated sources split in three files:
    * • package.scala: package object with common code(type aliases, etc)
    * • structures.scala: traits definitions, definitions of structures and generated InputFile structure
    * • methods.scala: methods case classes definitiion
    *
    * @param basePackage base package name
    * @param schema Telegram bot API structured schema
    * @return mapping from file name to sequence of sources
    */
  def generate(basePackage: String, schema: Schema): Map[String, Seq[String]] = {
    val packageName = packageDef(basePackage)
    val packObject  = packageObject(basePackage)

    val traitTrees = schema.baseTypes map baseTypeToTrait
//    println("========================== Traits")
//    traitTrees foreach println

    val structureTrees = schema.structs map structureToCaseClass
//    println("========================== Structures")
//    structureTrees foreach println

//    println("========================== BotApiRequest")
//    println(BotApiRequest)
    val methodTrees = schema.methods map methodToCaseClass
//    println("========================== Methods")
//    methodTrees foreach println

    Map(
      "package.scala" → packObject.map(_.syntax),
      "structures.scala" → (Seq(packageName, InputFile) ++ traitTrees ++ structureTrees)
        .map(_.syntax),
      "methods.scala" → (Seq(packageName, BotApiRequest) ++ methodTrees).map(_.syntax)
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
    val returnType = toScalaType(meth.responseType)

    q"final case class $methodName ( ..$params ) extends BotApiRequest[$returnType]"
  }

  private val InputFile: Defn.Class = q"final case class InputFile(fileId: String)"

  // base type for all bot API methods
  private val BotApiRequest: Defn.Trait = q"sealed trait BotApiRequest[Resp]"

  /**
    * Produce package name for given package
    * It contains common code used across structures and methods.
    *
    * @param basePackage package name
    * @return package for object
    */
  private def packageObject(basePackage: String) = {
    val stats = Seq(
      q"type ChatId = Either[Long, String]"
    )

    val (optPackage, packObjName) = (basePackage split "\\.").toList match {
      case init :+ last   ⇒ Some(packageDef(init mkString ".")) → last
      case List() :+ last ⇒ None                                → last
    }

    val packObj = {
      val name  = Term.Name(packObjName)
      val templ = template"{ ..$stats } "
      q"package object $name extends $templ"
    }

    optPackage.fold(Seq[Stat](packObj))(p ⇒ Seq(p, packObj))
  }

  private def packageDef(name: String): Pkg = {
    val p = Term.Name(name)
    q"package $p { }"
  }

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
  private def toScalaType(parsedType: ParsedType): Type = {
    def alias(pt: ParsedType): Option[Type] = pt match {
      case OrType(LiteralType("Integer" | "Int"), LiteralType("String")) ⇒ Some(t"ChatId")
      case _                                                             ⇒ None
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

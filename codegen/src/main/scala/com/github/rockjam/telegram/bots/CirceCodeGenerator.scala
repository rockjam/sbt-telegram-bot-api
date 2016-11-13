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
import scala.meta._

object CirceCodeGenerator extends TypeFunctions {

  import ScalametaCommon._

  /**
    * Generate source code of circe encoders and decoders for Telegram bot API models.
    *
    * We need encoders for all structures. For base types representing structures we need to generate untagged encoders.
    * We also need decoders for structures that present as method's response type, if they are structural types/contain structural types
    *
    * We should generate encoders for all methods(requests).
    * We should generate custom encoding for traits: pattern match all children and call child.asJson on it.
    *
    * @param schema Telegram bot API structured schema
    * @param basePackage base package name
    * @param modelsPackage name of package where models are stored
    * @return
    */
  def generate(schema: Schema,
               basePackage: String,
               modelsPackage: String): Map[String, Seq[String]] = {
    val se  = structuresEncoders(schema.structs, modelsPackage)
    val me  = methodsEncoders(schema.methods, modelsPackage)
    val mrd = methodResponseDecoders(schema.structs, schema.methods, modelsPackage)

    val pack = packageDef(basePackage)
    Map(
      "StructuresEncoders.scala"     → Seq(pack.syntax, se.syntax),
      "MethodsEncoders.scala"        → Seq(pack.syntax, me.syntax),
      "MethodResponseDecoders.scala" → Seq(pack.syntax, mrd.syntax)
    )
  }

  /**
    * Produce encoders for structures. Pack them in trait.
    *
    * For structures that have base type, generate base type encoder. It encodes children without tag.
    * For all structures generate encoder.
    *
    * @param structs all structures from Telegram bot API
    * @param modelsPackage package where all models are
    * @return trait containing encoders for all structures
    */
  private def structuresEncoders(structs: Seq[Structure], modelsPackage: String): Defn.Trait = {
    val modelsImport = singleWildcardImport(modelsPackage)

    val structureBaseEncoders = {
      // only those structures, that have base type
      val baseChildren = structs.groupBy(_.baseType) collect {
        case (Some(t), ss) ⇒ t → (ss map (_.name))
      }

      (baseChildren map {
        case (baseType, children) ⇒
          val encoderName =
            Pat.Var.Term(Term.Name(s"${StringUtils.lowerize(baseType.name)}Encoder"))
          val encoderType = Type.Name(baseType.name)

          val cases = children map { childName ⇒
            val typeName = Type.Name(childName)
            p"case x: $typeName => x.asJson"
          }

          q"""
        implicit val $encoderName: Encoder[$encoderType] = new Encoder[$encoderType] {
          def apply(a: $encoderType): Json = a match { ..case $cases }
        }
      """
      }).toSeq
    }

    val structEncoders = structs map {
      case Structure(name, _, _) ⇒
        circeEncoder(
          namePrefix = StringUtils.lowerize(name),
          encoderType = name
        )
    }

    val stats: Seq[Stat] = Seq(modelsImport, CirceCommonImports, DerivationConfigDecl) ++
        structureBaseEncoders ++
        structEncoders
    q"trait StructuresEncoders { ..$stats }"
  }

  /**
    * Produce encoders for methods. Pack them in trait.
    *
    * BotApiRequest is base type of all methods. Generate encoder for this base type.
    * It will encode children in untagged representation.
    *
    * For all methods generate encoders.
    *
    * @param meths all methods from Telegram bot API
    * @param modelsPackage package where all models are
    * @return trait containing encoders for all methods
    */
  private def methodsEncoders(meths: Seq[Method], modelsPackage: String): Defn.Trait = {
    val modelsImport = singleWildcardImport(modelsPackage)

    val botApiRequestEncoder = {
      val cases = meths map {
        case Method(name, _, _) ⇒
          val typeName = Type.Name(name.capitalize)
          p"case x: $typeName => x.asJson"
      }

      q"""
        implicit val botApiRequestEncoder: Encoder[BotApiRequest] = new Encoder[BotApiRequest] {
          def apply(a: BotApiRequest): Json = a match { ..case $cases }
        }
      """
    }

    val methsEncoders = meths map {
      case Method(name, _, _) ⇒
        val lowname  = name
        val typeName = name.capitalize

        circeEncoder(
          namePrefix = lowname,
          encoderType = typeName
        )
    }

    val stats: Seq[Stat] = Seq(
        modelsImport,
        CirceCommonImports,
        DerivationConfigDecl,
        botApiRequestEncoder,
        InputFileEncoder) ++ methsEncoders
    q"trait MethodsEncoders { ..$stats }"
  }

  /**
    * Produce decoders for method's response types. Pack them in trait
    * Response types are usually structures or literal types. Decoders for literal types we get for free.
    * We also don't need to produce decoders for all structures in schema,
    * cause we know exact response types for all methods.
    *
    * @note if `method.responseType` is a structure, its fields type may be structural type. So we need to generate decoder for this type too.
    *
    * @param structs all structures from Telegram bot API
    * @param meths all methods from Telegram bot API
    * @param modelsPackage package where all models are
    * @return trait containing decoders for all structural response types, and structural types they depend on.
    */
  private def methodResponseDecoders(structs: Seq[Structure],
                                     meths: Seq[Method],
                                     modelsPackage: String) = {
    val modelsImport = singleWildcardImport(modelsPackage)

    val decoders = {
      val allResponseTypeStructNames = {
        val respTypes = meths map (_.responseType)
        extractStructNamesDeep(
          initialStructNames = (respTypes flatMap extractStructNames).toSet,
          allStructs = structs
        )
      }

      allResponseTypeStructNames map { name ⇒
        circeDecoder(
          namePrefix = StringUtils.lowerize(name),
          decoderType = name
        )
      }
    }

    val stats: Seq[Stat] = Seq(
        modelsImport,
        CirceCommonImports,
        DerivationConfigDecl,
        ApiResponseDecoder) ++ decoders
    q"trait MethodResponseDecoders { ..$stats }"

  }

  /**
    * Produce circe `Encoder`
    *
    * Name prefix should be lowerized
    * Encoder type should be capitalized
    *
    * @param namePrefix encoder name prefix
    * @param encoderType encoder type
    * @return Definition of encoder
    */
  private def circeEncoder(namePrefix: String, encoderType: String): Defn.Val = {
    val encoderName = Pat.Var.Term(Term.Name(s"${namePrefix}Encoder"))
    q"implicit val $encoderName: Encoder[${Type.Name(encoderType)}] = deriveEncoder"
  }

  /**
    * Produce circe `Decoder`
    *
    * Name prefix should be lowerized
    * Decoder type should be capitalized
    *
    * @param namePrefix decoder name prefix
    * @param decoderType decoder type
    * @return Definition of decoder
    */
  private def circeDecoder(namePrefix: String, decoderType: String): Defn.Val = {
    val decoderName = Pat.Var.Term(Term.Name(s"${namePrefix}Decoder"))
    q"implicit val $decoderName: Decoder[${Type.Name(decoderType)}] = deriveDecoder"
  }

  // definition of ApiResponse decoder
  private val ApiResponseDecoder: Defn.Def =
    q"implicit def apiResponseDecoder[T: Decoder]: Decoder[ApiResponse[T]] = deriveDecoder"

  // definition of InputFile encoder
  private val InputFileEncoder: Defn.Val = circeEncoder("inputFile", "InputFile")

  private val CirceCommonImports: Import = {
    val i = Seq(
      importer"_root_.io.circe._",
      importer"_root_.io.circe.syntax._",
      importer"_root_.io.circe.generic.extras.semiauto._",
      importer"_root_.io.circe.generic.extras.Configuration"
    )
    q"import ..$i"
  }

  // declaration of derivation config. Required to produce json with snake cased keys
  private val DerivationConfigDecl: Decl.Val = q"implicit val derivationConfig: Configuration"

}

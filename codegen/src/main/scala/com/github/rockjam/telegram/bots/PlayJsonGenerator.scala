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

// TODO: cleanup and documentation.
object PlayJsonGenerator extends TypeFunctions {

  import ScalametaCommon._

  def generate(schema: Schema,
               basePackage: String,
               modelsPackage: String): Map[String, Seq[String]] = {
    val sw  = structuresWrites(schema.structs, modelsPackage)
    val mw  = methodsWrites(schema.methods, modelsPackage)
    val mrr = methodResponseReads(schema.structs, schema.methods, modelsPackage)

    val pack = packageDef(basePackage)
    Map(
      "StructuresWrites.scala"    → Seq(pack.syntax, sw.syntax),
      "MethodsWrites.scala"       → Seq(pack.syntax, mw.syntax),
      "MethodResponseReads.scala" → Seq(pack.syntax, mrr.syntax)
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
  private def structuresWrites(structs: Seq[Structure], modelsPackage: String): Defn.Trait = {
    val modelsImport = singleWildcardImport(modelsPackage)

    val structWrites = structs map {
      case Structure(name, fields, _) ⇒
        val lowname  = StringUtils.lowerize(name)
        val typeName = name

        makeWrites(lowname, typeName, fields.length)
    }

    val structureBaseWrites = {
      // only those structures, that have base type
      val baseChildren = structs.groupBy(_.baseType) collect {
        case (Some(t), ss) ⇒ t → (ss map (_.name))
      }

      (baseChildren map {
        case (baseType, children) ⇒
          val writesName = Pat.Var.Term(getWritesName(StringUtils.lowerize(baseType.name)))
          val writesType = Type.Name(baseType.name)

          val cases = children map { childName ⇒
            writesCase(
              name = StringUtils.lowerize(childName),
              typeName = childName
            )
          }

          q"""
          implicit val $writesName: Writes[$writesType] = new Writes[$writesType] {
            def writes(o: $writesType): JsValue = o match { ..case $cases }
          }
        """
      }).toSeq
    }

    val stats: Seq[Stat] = Seq(modelsImport, PlayJsonCommonImports, EmptyWrites) ++
        structureBaseWrites ++
        structWrites
    q"trait StructuresWrites { ..$stats }"
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
  private def methodsWrites(meths: Seq[Method], modelsPackage: String): Defn.Trait = {
    val modelsImport = singleWildcardImport(modelsPackage)

    val botApiRequestWrites = {
      val cases = meths map {
        case Method(name, _, _) ⇒
          writesCase(name, name.capitalize)
      }

      q"""
        implicit val botApiRequestWrites: Writes[BotApiRequest] = new Writes[BotApiRequest] {
          def writes(o: BotApiRequest): JsValue = o match { ..case $cases }
        }
      """
    }

    val methWrites = meths map {
      case Method(name, _, fields) ⇒
        val lowname  = name
        val typeName = name.capitalize
        makeWrites(lowname, typeName, fields.length)
    }

    val stats: Seq[Stat] = Seq(
        modelsImport,
        PlayJsonCommonImports,
        EmptyWrites,
        botApiRequestWrites,
        InputFileWrites) ++ methWrites
    q"trait MethodsWrites { ..$stats }"
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
  private def methodResponseReads(structs: Seq[Structure],
                                  meths: Seq[Method],
                                  modelsPackage: String) = {
    val modelsImport = singleWildcardImport(modelsPackage)

    val reads = {
      val allResponseTypeStructNames = {
        val respTypes = meths map (_.responseType)
        extractStructNamesDeep(
          initialStructNames = (respTypes flatMap extractStructNames).toSet,
          allStructs = structs
        )
      }

      allResponseTypeStructNames map { name ⇒
        playJsonReads(
          namePrefix = StringUtils.lowerize(name),
          typeName = name
        )
      }
    }

    val playFunctionalImport = singleWildcardImport("_root_.play.api.libs.functional.syntax")
    val stats: Seq[Stat] = Seq(
        modelsImport,
        PlayJsonCommonImports,
        playFunctionalImport,
        ApiResponseReads) ++ reads
    q"trait MethodResponseReads { ..$stats }"
  }

  /**
    * name should be lowerized
    * type name should be capitalized
    *
    * @param name ??? or name prefix
    * @param typeName
    * @return
    */
  private def writesCase(name: String, typeName: String): Case = {
    val writesName = arg"${getWritesName(name)}"
    val writesType = Type.Name(typeName)
    p"case x: $writesType => Json.toJson[$writesType](x)($writesName)"
  }

  // TODO: writes for structures with 22+ fields
  private def makeWrites(namePrefix: String, typeName: String, numberOfFields: Int) = {
    def playJsonEmptyWrites(namePrefix: String, typeName: String): Defn.Val = {
      val writesName = Pat.Var.Term(getWritesName(namePrefix))
      val writesType = Type.Name(typeName)
      q"implicit val $writesName: Writes[$writesType] = emptyWrites"
    }

    if (numberOfFields == 0) { // for empty case classes macro writes generator doesn't work.
      playJsonEmptyWrites(
        namePrefix = namePrefix,
        typeName = typeName
      )
    }
    //        for case classes with 22+ fields macro writes generator doesn't work
    //        else if(fields.length > 22) {
    //
    //        }
    else {
      playJsonWrites(
        namePrefix = namePrefix,
        typeName = typeName
      )
    }
  }

  /**
    * Produce circe `Encoder`
    *
    * Name prefix should be lowerized
    * Encoder type should be capitalized
    *
    * @param namePrefix encoder name prefix
    * @param typeName encoder type
    * @return Definition of encoder
    */
  private def playJsonWrites(namePrefix: String, typeName: String): Defn.Val = {
    val writesName = Pat.Var.Term(getWritesName(namePrefix))
    val writesType = Type.Name(typeName)
    q"implicit val $writesName: Writes[$writesType] = JsonNaming.snakecase(Json.writes[$writesType])"
  }

  // TODO: get?
  private def getWritesName(namePrefix: String): Term.Name =
    Term.Name(s"${namePrefix}Writes")

  /**
    * Produce circe `Decoder`
    *
    * Name prefix should be lowerized
    * Decoder type should be capitalized
    *
    * @param namePrefix decoder name prefix
    * @param typeName decoder type
    * @return Definition of decoder
    */
  private def playJsonReads(namePrefix: String, typeName: String): Defn.Val = {
    val readsName = Pat.Var.Term(Term.Name(s"${namePrefix}Decoder"))
    val readsType = Type.Name(typeName)
    q"implicit val $readsName: Reads[$readsType] = JsonNaming.snakecase(Json.reads[$readsType])"
  }

  // definition of ApiResponse reads
  private val ApiResponseReads: Defn.Def =
    q"""
      implicit def apiResponseReads[T: Reads]: Reads[ApiResponse[T]] =
        JsonNaming.snakecase(
          (
            (JsPath \ "ok").read[Boolean] and
            (JsPath \ "result").readNullable[T] and
            (JsPath \ "description").readNullable[String] and
            (JsPath \ "errorCode").readNullable[Int]
          )(ApiResponse.apply(_, _: Option[T], _, _)))
    """

  // definition of InputFile writes
  private val InputFileWrites: Defn.Val = playJsonWrites("inputFile", "InputFile")

  private val PlayJsonCommonImports: Import = {
    val i = Seq(
      importer"_root_.play.api.libs.json._",
      importer"_root_.com.github.tototoshi.play.json.JsonNaming"
    )
    q"import ..$i"
  }

  private val EmptyWrites: Defn.Def =
    q"private def emptyWrites[T] = Writes[T](_ ⇒ JsObject.apply(Seq.empty))"

}

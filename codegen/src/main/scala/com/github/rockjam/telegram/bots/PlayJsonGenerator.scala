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
    * Produce Writes for structures. Pack them in trait.
    *
    * For structures that have base type, generate base type Writes. It will write children without tag.
    * For all structures generate Writes.
    *
    * @param structs       all structures from Telegram bot API
    * @param modelsPackage package where all models are
    * @return trait containing Writes for all structures
    */
  private def structuresWrites(structs: Seq[Structure], modelsPackage: String): Defn.Trait = {
    val modelsImport = singleWildcardImport(modelsPackage)

    val structWrites = structs map {
      case Structure(name, fields, _) ⇒
        val lowname  = StringUtils.lowerize(name)
        val typeName = name

        makeWrites(lowname, typeName, fields)
    }

    val structureBaseWrites = {
      // only those structures, that have base type
      val baseChildren = structs.groupBy(_.baseType) collect {
        case (Some(t), ss) ⇒ t → (ss map (_.name))
      }

      (baseChildren map {
        case (baseType, children) ⇒
          baseWrites(baseType.name, children)
      }).toSeq
    }

    val stats: Seq[Stat] = Seq(modelsImport, CommonImports, EmptyWrites) ++
        structureBaseWrites ++
        structWrites
    q"trait StructuresWrites { ..$stats }"
  }

  /**
    * Produce Writes for methods. Pack them in trait.
    *
    * BotApiRequest is base type of all methods. Generate Writes for this base type.
    * It will write children in untagged representation.
    *
    * For all methods generate Writes.
    *
    * @param meths         all methods from Telegram bot API
    * @param modelsPackage package where all models are
    * @return trait containing Writes for all methods
    */
  private def methodsWrites(meths: Seq[Method], modelsPackage: String): Defn.Trait = {
    val modelsImport = singleWildcardImport(modelsPackage)

    val botApiRequestWrites =
      baseWrites("BotApiRequest", meths map (_.name))

    val methWrites = meths map {
      case Method(name, _, fields) ⇒
        val lowname  = name
        val typeName = name.capitalize
        makeWrites(lowname, typeName, fields)
    }

    val stats: Seq[Stat] = Seq(
        modelsImport,
        CommonImports,
        EmptyWrites,
        botApiRequestWrites,
        InputFileWrites) ++ methWrites
    q"trait MethodsWrites { ..$stats }"
  }

  /**
    * Produce Reads for method's response types. Pack them in trait
    * Response types are usually structures or literal types. Reads for literal types we get for free.
    * We also don't need to produce Reads for all structures in schema,
    * cause we know exact response types for all methods.
    *
    * @note if `method.responseType` is a structure, its fields type may be structural type.
    *       So we need to generate Reads for this type too.
    *
    * @param structs       all structures from Telegram bot API
    * @param meths         all methods from Telegram bot API
    * @param modelsPackage package where all models are
    * @return trait containing Reads for all structural response types, and structural types they depend on.
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

      structs.filter(s ⇒ allResponseTypeStructNames.contains(s.name)) map { struct ⇒
        makeReads(
          namePrefix = StringUtils.lowerize(struct.name),
          typeName = struct.name,
          fields = struct.fields
        )
      }
    }

    val stats: Seq[Stat] = Seq(modelsImport, CommonImports, ApiResponseReads) ++ reads
    q"trait MethodResponseReads { ..$stats }"
  }

  /**
    * make Writes for entity.
    *
    * Writes in play-json handled specially for entities with 0 fields
    * and entities with more than 22 fields. For this cases we generate special Writes.
    *
    * @note
    *       Name prefix should be lowerized
    *       Type name  should be capitalized
    *
    * @param namePrefix writes name prefix
    * @param typeName writes type
    * @param fields entity fields
    * @return definition of Writes
    */
  private def makeWrites(namePrefix: String, typeName: String, fields: Seq[Field]): Defn.Val = {
    def playJsonEmptyWrites(namePrefix: String, typeName: String): Defn.Val = {
      val writesName = Pat.Var.Term(getWritesName(namePrefix))
      val writesType = Type.Name(typeName)
      q"implicit val $writesName: Writes[$writesType] = emptyWrites"
    }

    if (fields.isEmpty) {
      // for empty case classes macro writes generator doesn't work.
      playJsonEmptyWrites(
        namePrefix = namePrefix,
        typeName = typeName
      )
    } else if (fields.length > 22) {
      // for case classes with 22+ fields macro writes generator doesn't work
      writes22Plus(
        namePrefix = namePrefix,
        typeName = typeName,
        fields = fields
      )
    } else {
      writes(
        namePrefix = namePrefix,
        typeName = typeName
      )
    }
  }

  /**
    * Produce `Writes`
    *
    * @note
    *       Name prefix should be lowerized
    *       Writes type should be capitalized
    *
    * @param namePrefix writes name prefix
    * @param typeName   writes type
    * @return Definition of writes
    */
  private def writes(namePrefix: String, typeName: String): Defn.Val = {
    val writesName = Pat.Var.Term(getWritesName(namePrefix))
    val writesType = Type.Name(typeName)
    q"implicit val $writesName: Writes[$writesType] = JsonNaming.snakecase(Json.writes[$writesType])"
  }

  /**
    * Produce `Writes` for entity with 22+ fields
    *
    * @param namePrefix writes name prefix
    * @param typeName writes type
    * @param fields entity fields
    * @return Definition of writes
    */
  private def writes22Plus(namePrefix: String, typeName: String, fields: Seq[Field]): Defn.Val = {
    val writesName = Pat.Var.Term(getWritesName(namePrefix))
    val writesType = Type.Name(typeName)
    val format     = format22Plus(namePrefix, typeName, fields)

    q"implicit val $writesName: Writes[$writesType] = $format"
  }

  private def getWritesName(namePrefix: String): Term.Name =
    Term.Name(s"${namePrefix}Writes")

  /**
    * make `Reads` for entity.
    *
    * `Reads` in play-json handled specially for entities with more than 22 fields.
    * For this case we generate special `Reads`.
    *
    * @note
    *       Name prefix should be lowerized
    *       Type name  should be capitalized
    *
    * @param namePrefix reads name prefix
    * @param typeName reads type
    * @param fields entity fields
    * @return definition of `Reads`
    */
  private def makeReads(namePrefix: String, typeName: String, fields: Seq[Field]): Defn.Val =
    if (fields.length > 22) {
      reads22Plus(
        namePrefix = namePrefix,
        typeName = typeName,
        fields = fields
      )
    } else {
      reads(
        namePrefix = namePrefix,
        typeName = typeName
      )
    }

  /**
    * Produce `Reads`
    *
    * @note
    *       Name prefix should be lowerized
    *       `Reads` type should be capitalized
    *
    * @param namePrefix reads name prefix
    * @param typeName   reads type
    * @return Definition of reads
    */
  private def reads(namePrefix: String, typeName: String): Defn.Val = {
    val readsName = Pat.Var.Term(getReadsName(namePrefix))
    val readsType = Type.Name(typeName)
    q"implicit val $readsName: Reads[$readsType] = JsonNaming.snakecase(Json.reads[$readsType])"
  }

  /**
    * Produce `Reads` for entity with 22+ fields
    *
    * @param namePrefix reads name prefix
    * @param typeName reads type
    * @param fields entity fields
    * @return Definition of reads
    */
  private def reads22Plus(namePrefix: String, typeName: String, fields: Seq[Field]) = {
    val readsName = Pat.Var.Term(getReadsName(namePrefix))
    val readsType = Type.Name(typeName)
    val format    = format22Plus(namePrefix, typeName, fields)

    q"implicit val $readsName: Reads[$readsType] = $format"
  }

  private def getReadsName(namePrefix: String): Term.Name = Term.Name(s"${namePrefix}Reads")

  /**
    * Produce `Format` for entity with 22+ fields
    * `Format` can act as both `Reads` and `Writes`
    *
    * @param namePrefix format name prefix
    * @param typeName format type
    * @param fields entity fields
    * @return Definition of format
    */
  private def format22Plus(namePrefix: String, typeName: String, fields: Seq[Field]): Term.Block = {
    def getGroupedFormatName(groupIndex: Int) =
      Term.Name(s"${namePrefix}Format${groupIndex}")

    // `groupedFieldNames` - names of entity fields
    // `groupedFormats` - definition of format grouped by 21 fields.
    val (groupedFieldNames, groupedFormats) = (fields.grouped(21).zipWithIndex map {
      case (fieldsGroup, groupIndex) ⇒
        val (fieldNames, groupTypes, fieldFormats) = (fieldsGroup map { field ⇒
          val fieldName = field.name

          field.typ match {
            // maybe not the best approach. Should we work with schema's parsed type ???
            case ot: OptionType ⇒
              val fieldType = CodeGenerator.toScalaType(ot)
              val innerType = CodeGenerator.toScalaType(ot.tp)
              (
                StringUtils.camelize(field.name),
                fieldType,
                q"(JsPath \ $fieldName).formatNullable[$innerType]"
              )
            case t ⇒
              val scalaType = CodeGenerator.toScalaType(t)
              (
                StringUtils.camelize(field.name),
                scalaType,
                q"(JsPath \ $fieldName).format[$scalaType]"
              )
          }
        }).unzip3

        // TODO: move to common function
        val groupedFormat = fieldFormats match {
          case head +: tail ⇒
            (tail foldLeft (head: Term)) { case (acc, el) ⇒ arg"${acc}.and(${el})" }
          case Seq() ⇒
            sys.error("Can't combine empty sequence of terms")
        }

        val groupedFormatName = Pat.Var.Term(getGroupedFormatName(groupIndex))

        fieldNames → q"val $groupedFormatName: OFormat[( ..$groupTypes )] = ${groupedFormat}.tupled"
    }).toVector.unzip

    val entityFormat = {
      val completeFormat = {
        val formatNames = groupedFormats.zipWithIndex map {
          case (_, i) ⇒ getGroupedFormatName(i)
        }
        // TODO: move to common function
        formatNames match {
          case head +: tail ⇒
            (tail foldLeft (head: Term)) { case (acc, el) ⇒ q"${acc}.and($el)" }
          case Seq() ⇒
            sys.error("Can't combine empty sequence of terms")
        }
      }

      val toEntityFunction = {
        val cases = {
          val tuples = groupedFieldNames map { names ⇒
            val ns = names map { n ⇒
              Pat.Var.Term(Term.Name(n))
            }
            p"( ..$ns )"
          }

          val constructCall = {
            val args = groupedFieldNames.flatten map { fn ⇒
              val fname = Term.Name(fn)
              arg"$fname"
            }
            val tn = Ctor.Name(typeName)
            ctor"$tn( ..$args )"
          }

          Vector(p"case ( .. $tuples) => $constructCall")
        }

        q"{ ..case $cases }"
      }

      val fromEntityFunction = {
        val entityName = Term.Name(namePrefix)

        val tupledFieldCalls = groupedFieldNames map { group ⇒
          val fieldCalls = group map { fieldName ⇒
            q"${entityName}.${Term.Name(fieldName)}"
          }
          q"( ..$fieldCalls )"
        }

        val entityParam = param"${entityName}:${Type.Name(typeName)}"

        q"($entityParam) => ( ..$tupledFieldCalls )"
      }

      val formatApplication = q"${completeFormat}.apply($toEntityFunction, $fromEntityFunction)"
      q"{ ..$groupedFormats;  $formatApplication }"
    }
    entityFormat
  }

  /**
    * `Writes` for base type and it's cases
    *
    * @param baseType name of base type
    * @param children sequence of child names
    * @return Definition of writes for base class
    */
  private def baseWrites(baseType: String, children: Seq[String]): Defn.Val = {
    val writesName = Pat.Var.Term(getWritesName(StringUtils.lowerize(baseType)))
    val writesType = Type.Name(baseType.capitalize)

    val cases = children map { childName ⇒
      val cwName = arg"${getWritesName(StringUtils.lowerize(childName))}"
      val cwType = Type.Name(childName.capitalize)
      p"case x: $cwType => Json.toJson[$cwType](x)($cwName)"
    }

    q"""
      implicit val $writesName: Writes[$writesType] = new Writes[$writesType] {
        def writes(o: $writesType): JsValue = o match { ..case $cases }
      }
    """
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
  private val InputFileWrites: Defn.Val = writes("inputFile", "InputFile")

  private val CommonImports: Import = {
    val i = Seq(
      importer"_root_.play.api.libs.json._",
      importer"_root_.play.api.libs.functional.syntax._",
      importer"_root_.com.github.tototoshi.play.json.JsonNaming"
    )
    q"import ..$i"
  }

  private val EmptyWrites: Defn.Def =
    q"private def emptyWrites[T] = Writes[T](_ ⇒ JsObject.apply(Seq.empty))"

}

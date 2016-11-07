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

import org.jsoup.Jsoup
import org.jsoup.nodes.{ Document, Element }
import org.jsoup.select.Elements

import scala.collection.JavaConverters._

object SchemaParser {

  // TODO: surround with try catch
  def parse(url: String): Schema = {
    val doc = Jsoup.connect(url).get

    val (structureDocs, methodDocs, baseTypeDocs) = {
      val zero = {
        val ev = Vector.empty[(String, Vector[Element])]
        (ev, ev, ev)
      }

      (structuresAndMethods(doc) foldLeft zero) {
        case ((structs, meths, baseTypes), el @ (name, dom)) ⇒
          if (isMethod(name)) {
            (
              structs,
              meths :+ el,
              baseTypes
            )
          } else {
            if (isBaseType(dom))
              (
                structs,
                meths,
                baseTypes :+ el
              )
            else
              (
                structs :+ el,
                meths,
                baseTypes
              )
          }
      }
    }

    val baseTypeNames = baseTypeDocs.map(_._1)

    val structNames = structureDocs.map(_._1) ++ baseTypeNames

    val structures = {
      // Map from concrete type to base type
      val baseTypeReverseIndex: Map[String, String] =
        (baseTypeDocs flatMap {
          case (name, elements) ⇒
            extractBaseTypeChildren(elements) map (_ → name)
        }).toMap

      structureDocs filter (_._1 != "InputFile") map {
        case (name, elements) ⇒
          Structure(
            name,
            fields = extractStructFields(elements, structNames),
            baseType = baseTypeReverseIndex.get(name)
          )
      }
    }

    val methods = methodDocs map {
      case (name, elements) ⇒
        Method(
          name,
          returnTyp = extractMethodResponseType(name, elements, structNames),
          fields = extractMethodFields(elements, structNames)
        )

    }

    val structureBases = baseTypeNames map StructureBase.apply

    println(s"===Structures:")
    structures foreach { s ⇒
      println(s"====>> ${s.name}")
      s.fields foreach println
    }

    println(s"===Methods:")
    methods foreach { m ⇒
      println(s"====>> ${m.name}")
      println(s"====>> ${m.returnTyp}")
      m.fields foreach println
    }

    Schema(structures, structureBases, methods)
  }

  /**
    * Assuming that method name starts with lower case letter
    * isMethod("Update") -> false
    * isMethod("getMe") -> true
    *
    * @param s
    * @return
    */
  private def isMethod(s: String) = s(0).isLower

  private def isBaseType(es: Seq[Element]) = {
    val linksInsideLi = baseTypeStructLinks(es)
    linksInsideLi.nonEmpty && linksInsideLi.forall(e ⇒ !e.text.contains(" ")) // TODO: is there single method for this?
  }

  private def extractBaseTypeChildren(es: Seq[Element]) =
    baseTypeStructLinks(es) map (_.text)

  private def baseTypeStructLinks(es: Seq[Element]): Seq[Element] =
    new Elements(es.asJava).select("ul li a").asScala

  // returns structures and methods
  // h4 -> Seq(p, tr, p)
  // returns DOM
  private def structuresAndMethods(doc: Document): Vector[(String, Vector[Element])] = {

    val pageContent: Seq[Element] = doc.select("#dev_page_content").first.children.asScala

    (pageContent foldLeft Vector.empty[(String, Vector[Element])]) {
      case (acc, el) ⇒
        el.tagName match {
          // it's better to filter out keys later
          case "h4" ⇒
            acc :+ (el.text → Vector.empty[Element])
          case other ⇒
            acc match {
              case Vector() ⇒
                println(s"===got other: ${el}")
                acc
              case init :+ ((title, elements)) ⇒
                init :+ (title → (elements :+ el))
            }
        }
    } filter { case (name, _) ⇒ name.nonEmpty && !name.contains(" ") }
  }

  // TODO: DRY
  //TODO: reduce number of conversions
  private def extractStructFields(
      elems: Seq[Element],
      structNames: Seq[String]): scala.collection.immutable.Seq[Field] = {
    val es = new Elements(elems.asJava)
    // TODO: match first tr, validate header
    (es.select("table tr").asScala flatMap { tr ⇒
      val tds: List[Element] = tr.children().asScala.toList
      tds map (_.text) match {
        case "Field" :: "Type" :: "Description" :: Nil ⇒ // maybe move it in validation step
          println("=========== got header")
          None
        case field :: typ :: desc :: Nil if desc contains "Optional" ⇒
          Some(
            Field(
              name = field,
              typ = extractType(typ, isOptional = true, structNames),
              desc
            )
          )
        case field :: rawType :: desc :: Nil ⇒
          Some(
            Field(
              name = field,
              typ = extractType(rawType, isOptional = false, structNames),
              desc
            )
          )
      }
    }).toVector
  }

  // TODO: DRY
  //TODO: reduce number of conversions
  private def extractMethodFields(
      elems: Seq[Element],
      structNames: Seq[String]): scala.collection.immutable.Seq[Field] = {
    val es = new Elements(elems.asJava)
    // TODO: match first tr, validate header
    (es.select("table tr").asScala flatMap { tr ⇒
      val tds: List[Element] = tr.children().asScala.toList
      tds map (_.text) match {
        case "Parameters" :: "Type" :: "Required" :: "Description" :: Nil ⇒ // maybe move it in validation step
          println("=========== got header")
          None
        case param :: typ :: required :: desc :: Nil if required == "Optional" ⇒
          Some(
            Field(
              name = param,
              typ = extractType(typ, isOptional = true, structNames),
              desc
            )
          )
        case param :: typ :: required :: desc :: Nil if required == "Yes" ⇒
          Some(
            Field(
              name = param,
              typ = extractType(typ, isOptional = false, structNames),
              desc
            )
          )
      }
    }).toVector
  }

  // example of method return type:
  //Returns True on success.
  //Returns a UserProfilePhotos object.
  //On success, a File object is returned
  //Returns a Chat object on success.
  //On success, returns an Array of ChatMember objects that contains information about all chat administrators except other bots
  //Returns Int on success.
  //Returns a ChatMember object on success.
  //On success, True is returned.
  //  An Array of <a href="#update">Update</a> objects is returned.
  //On success, if edited message is sent by the bot, the edited <a href="#message">Message</a> is returned, otherwise <em>True</em> is returned

  private def extractMethodResponseType(name: String,
                                        elems: Seq[Element],
                                        structNames: Seq[String]): ParsedType = {
    val DefaultType = LiteralType("True")

    val paragraphs = new Elements(elems.asJava).select("p").asScala

    // find sentence with "return" word among paragraphs we have.
    val sentences = paragraphs flatMap { p ⇒
      p.html.split("\\. ") // split on sentences
    } filter { s ⇒
      s.toLowerCase.contains("return") // find one with "return" word in it.
    }

    // if we didn't find exact and only 1 sentence, narrow search to sentences containing "on success"
    val optSentence = if (sentences.length > 1) {
      sentences.find(_.toLowerCase.contains("on success"))
    } else {
      sentences.headOption
    }

    optSentence map { s ⇒
      val doc = Jsoup.parseBodyFragment(s)

      // struct type have higher precedence.
      val literalType =
        doc.body.select("em").asScala.map(_.text).headOption.map(LiteralType.apply)
      val structType =
        doc.body.select("a").asScala.map(_.text).find(structNames.contains).map(StructType.apply)

      val isArrayType = s.contains("Array of")
      val isOrType    = s.contains("otherwise")

      if (isOrType) {
        OrType(structType.get, literalType.get)
      } else {
        val singleType: ParsedType =
          structType.orElse(literalType).getOrElse(sys.error(s"Unable to parse type from: ${s}"))

        if (isArrayType) ListType(singleType)
        else singleType
      }
    } getOrElse DefaultType
  }

  /**
    * Extracts type from raw type like:
    * Integer
    * String
    * Integer or String
    * Boolean
    * InlineKeyboardMarkup or ReplyKeyboardMarkup or ReplyKeyboardHide or ForceReply
    * Float
    * Float number
    * InlineKeyboardMarkup
    * Array of InlineQueryResult
    * Array of Array of PhotoSize
    *
    * @param rawType
    * @param isOptional
    * @param structNames
    * @return
    */
  private def extractType(rawType: String,
                          isOptional: Boolean,
                          structNames: Seq[String]): ParsedType = {
    val tokens = (rawType.trim split " ").filterNot(_ == "number").toList

    def aux(tokens: List[String]): ParsedType = tokens match {
      case tp :: Nil ⇒
        if (structNames.contains(tp)) StructType(tp)
        else LiteralType(tp)
      case "Array" :: "of" :: rest ⇒
        ListType(aux(rest))
      case tp :: "or" :: rest ⇒
        OrType(aux(tp :: Nil), aux(rest))
    }

    val t = aux(tokens)
    if (isOptional) OptionType(t) else t
  }

}

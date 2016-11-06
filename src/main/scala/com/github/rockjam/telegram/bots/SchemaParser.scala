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

    // should also extract base types here
    val (structureDocs, methodDocs) =
      structuresAndMethods(doc).partition(e ⇒ isStructure(e._1))

    val structNames = structureDocs map (_._1)

    val structures = structureDocs filter (_._1 != "InputFile") map {
      case (name, elements) ⇒
        Structure(
          name,
          fields = extractStructFields(name, elements, structNames)
        )
    }

    val methods = methodDocs map {
      case (name, elements) ⇒
        Method(
          name,
          returnTyp = extractMethodReturnType(elements),
          fields = extractMethodFields(name, elements, structNames)
        )

    }

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

    Schema(structures, methods)
  }

  /**
    * Assuming that structure name starts with Upper case
    * isStructure("Update") -> true
    * isStructure("getMe") -> false
    *
    * @param s
    * @return
    */
  private def isStructure(s: String) = s(0).isUpper

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
            acc :+ el.text → Vector.empty[Element]
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
      name: String,
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
      name: String,
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
  private def extractMethodReturnType(elems: Seq[Element]): ParsedType =
    LiteralType("String")

//    val returnType = otherNodes // other nodes are not table
//      .mkString(" ")
//      .split("\\.")
//      .toSeq
//      .filter(_.toLowerCase.contains("return"))
//      .map { result ⇒
//        val types = TypeExtractorPattern.findAllMatchIn(result).map {
//          _.group(0).replaceAll(HtmlTagPattern, "$1").trim
//        }
//        if (types.nonEmpty)
//          Some(types.mkString(" or "))
//        else
//          None
//      }
//      .find(_.nonEmpty)
//      .flatten
//      .getOrElse("True")

  /**
    * extract type from string like:
    * Integer
    * String
    * Integer or String
    * Boolean
    * InlineKeyboardMarkup or ReplyKeyboardMarkup or ReplyKeyboardHide or ForceReply
    * Float number
    * InlineKeyboardMarkup
    * Array of InlineQueryResult
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

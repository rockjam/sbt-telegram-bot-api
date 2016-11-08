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

object HTMLSchemaParser {

  /**
    * Parses html page from given URL to schema definition.
    * Schema definition is structured sequence of structures, types and methods.
    *
    * @param url Telegram bot API description URL
    * @return schema definition
    */
  def parse(url: String): Schema = {
    val doc = try {
      Jsoup.connect(url).get
    } catch {
      case e: Exception ⇒ throw new RuntimeException(s"Failed to fetch url: $url", e)
    }

    val (structuresDOM, methodDOM, typesDOM) = {
      val zero = {
        val ev = Vector.empty[(String, Vector[Element])]
        (ev, ev, ev)
      }

      // split entities into structures, methods and types
      (extractEntities(doc) foldLeft zero) {
        case ((structs, meths, types), el @ (name, dom)) ⇒
          if (isMethod(name)) {
            (
              structs,
              meths :+ el,
              types
            )
          } else {
            if (isType(dom))
              (
                structs,
                meths,
                types :+ el
              )
            else
              (
                structs :+ el,
                meths,
                types
              )
          }
      }
    }

    val structNames = structuresDOM.map(_._1) ++ typesDOM.map(_._1)

    val (baseTypes, typeReverseMap) = {
      val (bts, revTypes) = (typesDOM map {
        case (name, elements) ⇒
          val bt = BaseType(name)
          bt → (extractTypeChildren(elements) map (_ → bt))
      }).unzip
      bts → revTypes.flatten.toMap
    }

    // InputFile is defined separately.
    val structures = structuresDOM filter (_._1 != "InputFile") map {
      case (name, elements) ⇒
        Structure(
          name,
          fields = extractStructFields(elements, structNames),
          baseType = typeReverseMap.get(name)
        )
    }

    val methods = methodDOM map {
      case (name, elements) ⇒
        Method(
          name,
          returnTyp = extractMethodResponseType(elements, structNames),
          fields = extractMethodFields(elements, structNames)
        )
    }

//    println(s"===Structures:")
//    structures foreach { s ⇒
//      println(s"====>> ${s.name}")
//      s.fields foreach println
//    }
//
//    println(s"===Methods:")
//    methods foreach { m ⇒
//      println(s"====>> ${m.name}")
//      println(s"====>> ${m.returnTyp}")
//      m.fields foreach println
//    }

    Schema(
      structures,
      baseTypes,
      methods
    )
  }

  /**
    * Extract DOM elements associated with structures, methods and types, presented in `doc`.
    *
    * #dev_page_content <div> contains description of methods, types and structures we need to extract.
    * h4 is header that usually contains structure/method/type name.
    * We run through document from top to bottom and
    * associate html elements with current h4 until next h4.
    *
    * @example {{{
    * val dom = s"""
    *    |<div id="dev_page_content">
    *    |  <h4>one</h4>
    *    |  <p>This is method one</p>
    *    |  <div>And it does a lot</div>
    *    |  <h4>Two</h4>
    *    |  <p>This is structure two</p>
    *    |  <div>And it does nothing</div>
    *    |</div>
    *  """.stripMargin
    * val doc = Jsoup.parse(dom)
    * extractEntities(doc)
    * res: : Vector[(String, Vector[org.jsoup.nodes.Element])] =
    *  Vector(
    *   ("one",Vector(<p>This is method one</p>, <div>And it does a lot</div>)),
    *   ("Two",Vector(<p>This is structure two</p>, <div>And it does nothing</div>))
    *  )
    * }}}
    *
    * After all, we filter out all empty keys, and keys containing whitespaces: those aren't structure/method/type names.
    *
    * @param doc html document describing Telegram bot API
    * @return sequence of method/structure/type name -> DOM elements associated with it
    */
  def extractEntities(doc: Document): Vector[(String, Vector[Element])] = {

    val pageContent: Seq[Element] = doc.select("#dev_page_content").first.children.asScala

    (pageContent foldLeft Vector.empty[(String, Vector[Element])]) {
      case (acc, el) ⇒
        el.tagName match {
          case "h4" ⇒
            acc :+ (el.text → Vector.empty[Element])
          case other ⇒
            acc match {
              case Vector() ⇒ acc
              case init :+ ((title, elements)) ⇒
                init :+ (title → (elements :+ el))
            }
        }
    } filter { case (name, _) ⇒ name.nonEmpty && !name.contains(" ") }
  }

  /**
    * Extracts structure's fields from DOM elements associated with given structure.
    * Fields description always contains in table(<table>).
    *
    * We extract table content from `elems`, validate table header,
    * and extract field name, type, and description.
    * @note description contains a hint, whether this field is optional.
    *
    * @param elems DOM elements associated with given structure
    * @param structNames names of all structures of Telegram bot API
    * @return sequence of structure's field descriptions
    */
  private def extractStructFields(
      elems: Seq[Element],
      structNames: Seq[String]): scala.collection.immutable.Seq[Field] =
    (fieldsTable(elems) match {
      // first line of table is header we just match and omit
      case ("Field" :: "Type" :: "Description" :: Nil) :: tail ⇒
        tail map {
          case field :: typ :: desc :: Nil if desc contains "Optional" ⇒
            Field(
              name = field,
              typ = extractType(typ, isOptional = true, structNames),
              desc
            )
          case field :: rawType :: desc :: Nil ⇒
            Field(
              name = field,
              typ = extractType(rawType, isOptional = false, structNames),
              desc
            )
        }
      case _ ⇒ List.empty
    }).toVector

  /**
    * Extracts method's parameters(fields) from DOM elements associated with given structure.
    * Parameters description contains in table(<table>).
    *
    * We extract table content from `elems`, validate table header,
    * and extract field name, type, info whether this field is optional or not, and description.
    *
    * @param elems DOM elements associated with given method
    * @param structNames names of all structures of Telegram bot API
    * @return sequence of method's field descriptions
    */
  private def extractMethodFields(
      elems: Seq[Element],
      structNames: Seq[String]): scala.collection.immutable.Seq[Field] =
    // first line of table is header we just match and omit
    (fieldsTable(elems) match {
      case ("Parameters" :: "Type" :: "Required" :: "Description" :: Nil) :: tail ⇒
        tail map {
          case param :: typ :: required :: desc :: Nil if required == "Optional" ⇒
            Field(
              name = param,
              typ = extractType(typ, isOptional = true, structNames),
              desc
            )
          case param :: typ :: required :: desc :: Nil if required == "Yes" ⇒
            Field(
              name = param,
              typ = extractType(typ, isOptional = false, structNames),
              desc
            )
        }
      case _ ⇒ List.empty
    }).toVector

  /**
    * If given entity is a type, extract names of children of this type, otherwise return empty sequence.
    * Type is defined by list(<ul> with bunch of <li>) of links(<a>) pointing to concrete structures of Telegram bot API.
    *
    * @param elems DOM elements associated with given entity
    * @return sequence of concrete structures names, possibly empty.
    */
  private def extractTypeChildren(elems: Seq[Element]): Seq[String] = {
    val links  = new Elements(elems.asJava).select("ul li a").asScala
    val isType = links.nonEmpty && links.forall(e ⇒ !e.text.contains(" ")) //TODO: is there single method for it?

    if (isType) links map (_.text)
    else Seq.empty
  }

  /**
    * Extracts method's response type from method description.
    * Method description usually contains in one of sentences of paragraph(<p>)
    * before table with fields description.
    *
    * All sentences with response type contains words "returns", "Returns", "returned".
    * In ambiguous cases(when 2 or more sentences contains "return" word), we can narrow search
    * to sentences containing "on success".
    *
    * Return type we need to extract is represented in two forms:
    * • Telegram bot API structure, that contains inside <a> tag
    * • literal type, contains inside <em> tag
    * @note `StructType` have higher precedence over `LiteralType` when both are found.
    *
    * `ListType` encoded in text as "Array of {Type}"
    * `OrType` encoded in text as "...{First Type}..., otherwise ...{Second Type}...".
    *
    * When no response type found in method description, we use defaul type: LiteralType("True")`
    *
    * @param elems DOM elements associated with given method
    * @param structNames names of all structures of Telegram bot API
    * @return parsed response type
    */
  private def extractMethodResponseType(elems: Seq[Element],
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

      val literalType =
        doc.body.select("em").asScala.map(_.text).headOption.map(LiteralType.apply)
      val structType =
        doc.body.select("a").asScala.map(_.text).find(structNames.contains).map(StructType.apply)

      val isArrayType = s.contains("Array of")
      val isOrType    = s.contains("otherwise")

      if (isOrType) {
        OrType(structType.get, literalType.get)
      } else {
        // struct type have higher precedence.
        val singleType: ParsedType =
          structType.orElse(literalType).getOrElse(sys.error(s"Unable to parse type from: ${s}"))

        if (isArrayType) ListType(singleType)
        else singleType
      }
    } getOrElse DefaultType
  }

  /**
    * Deconstructs raw type string into `ParsedType`.
    * `structNames` contains names of all structures in schema,
    * and used to find out whether type is `LiteralType`,
    * like String, Boolean or Float, or `StructType` like Chat, Update or Message.
    * If value of this type is optional, resulting type is wrapped with `OptionType`
    *
    * @param rawType raw string representation of type
    * @param isOptional is value of this type is Optional
    * @param structNames names of all structures of Telegram bot API
    * @return parsed type
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

  /**
    * Find out if given string is method, assuming that method
    * name starts with lower case letter.
    *
    * isMethod("Update") -> false
    * isMethod("getMe") -> true
    *
    * @param s string to test
    * @return is given string a method
    */
  private def isMethod(s: String) = s(0).isLower

  /**
    * Figure out if given entity is type.
    *
    * @param elems DOM elements associated with given entity
    * @return is given entity is type
    */
  private def isType(elems: Seq[Element]) =
    extractTypeChildren(elems).nonEmpty

  /**
    * Extracts rows and cells from html table,
    * that contains description of structure/method fields.
    *
    * @param elems sequence DOM elements
    * @return `List[ List[String] ]`, where outer list is list of rows, and inner lists are lists of columns
    */
  private def fieldsTable(elems: Seq[Element]): List[List[String]] = {
    // TODO: reduce number of conversions
    val es = new Elements(elems.asJava)
    es.select("table tr").asScala.map(_.children.asScala.map(_.text).toList).toList
  }

}

/**
 * Copyright (C) 2013 Alexander Azarov <azarov@osinka.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.osinka.subset

import org.bson.types.ObjectId
import com.mongodb.DBObject

import BsonParser.{ParseResult,Document}

object BsonParser {
  type ParseResult[+A] = Either[String,A]
  type Document = DBObject
}

case class ~[+A, +B](_1: A, _2: B)

trait DocParser[+A] extends (Document => ParseResult[A]) { parent =>
  def map[B](f: A => B): DocParser[B] = DocParser(parent.andThen(_.right.map(f)))

  def collect[B](otherwise: String)(f: PartialFunction[A, B]): DocParser[B] =
    DocParser(parent(_).right flatMap {a => f.lift(a).toRight(otherwise) })

  def flatMap[B](f: A => DocParser[B]): DocParser[B] = DocParser(doc => parent(doc).right flatMap (a => f(a)(doc)))

  def ~[B](p: DocParser[B]): DocParser[A ~ B] = DocParser(doc => parent(doc).right flatMap (a => p(doc).right map (new ~(a, _))))

  def ~>[B](p: DocParser[B]): DocParser[B] = DocParser(doc => parent(doc).right flatMap (a => p(doc)))

  def <~[B](p: DocParser[B]): DocParser[A] = parent.~(p).map(_._1)

  def |[B >: A](p: DocParser[B]): DocParser[B] = DocParser(doc => parent(doc) fold (_ => p(doc), a => Right(a)))

  def opt : DocParser[Option[A]] = DocParser(doc => Right(parent(doc).right.toOption))

  def >>[B](f: A => DocParser[B]): DocParser[B] = flatMap(f)

  /* parsers in pattern matching, when errors are not relevant
   *
   * ```
   * val parser: DocParser[Int] = ...
   * collection.find().asScala collect {
   *   case parser(i) => i
   * }
   * ```
   */
  def unapply(doc: Document): Option[A] = apply(doc).right.toOption

  /**
   * optimistic parsing, e.g. we assuming we can parse everything we get. Otherwise
   * we'll get exception
   *
   * ```
   * collection.find().asScala map {parser.parse}
   * ```
   */
  def parse: Document => A =
    this(_) fold (msg => throw new Exception(msg), x => x)
}

object DocParser {
  def apply[A](f: Document => ParseResult[A]): DocParser[A] =
    new DocParser[A] {
      def apply(dbo: Document): ParseResult[A] = f(dbo)
    }

  def get[T : Field](path: Seq[String]): DocParser[T] =
    path match {
      case Seq(name) => get[T](name)
      case _ => doc[T](path.head)(get[T](path.tail))
    }

  def get[T](name: String)(implicit f: Field[T]): DocParser[T] =
    DocParser[T] { doc =>
      Option(doc.get(name)) flatMap (f.apply(_)) toRight ("No field `"+name+"`")
    }

  def doc[T](name: String)(parser: DocParser[T])(implicit f: Field[DBObject]): DocParser[T] =
    get[DBObject](name).flatMap(doc => DocParser[T](_ => parser(doc)))

  def int(name: String)(implicit f: Field[Int]): DocParser[Int] = get[Int](name)

  def str(name: String)(implicit f: Field[String]): DocParser[String] = get[String](name)

  def bool(name: String)(implicit f: Field[Boolean]): DocParser[Boolean] = get[Boolean](name)

  def long(name: String)(implicit f: Field[Long]): DocParser[Long] = get[Long](name)

  def sym(name: String)(implicit f: Field[Symbol]): DocParser[Symbol] = get[Symbol](name)

  def oid(name: String)(implicit f: Field[ObjectId]): DocParser[ObjectId] = get[ObjectId](name)

  def contains[T : Field](name: String, t: T): DocParser[Unit] =
    get[T](name).collect("No field `"+name+"` with value `"+t+"`") { case a if a == t => Unit }

  def fails(msg: String) = DocParser[Unit](_ => Left(msg))

  def docId(implicit f: Field[ObjectId]): DocParser[ObjectId] = oid("_id")
}
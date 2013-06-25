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

import annotation.implicitNotFound
import java.util.Date
import java.util.regex.Pattern
import util.matching.Regex
import org.bson.types.{ObjectId, Binary, Symbol => BsonSymbol}
import com.mongodb.DBObject

object BsonParser {
  type ParseResult[+A] = Either[String,A]
  type Document = DBObject
}

import BsonParser.{ParseResult,Document}

case class ~[+A, +B](_1: A, _2: B)

@implicitNotFound(msg = "Cannot find Field for ${A}")
trait Field[+A] { parent =>
  def apply(o: Any): ParseResult[A]

  def map[B](f: A => B): Field[B] = FieldF[B](o => parent.apply(o).right map f)

  def collect[B](otherwise: String)(f: PartialFunction[A, B]): Field[B] =
    FieldF(parent(_).right flatMap {a => f.lift(a).toRight(otherwise) })

  def flatMap[B](f: A => Field[B]): Field[B] = FieldF(doc => parent(doc).right flatMap (a => f(a)(doc)))

  def ~[B](p: Field[B]): Field[A ~ B] = FieldF(doc => parent(doc).right flatMap (a => p(doc).right map (new ~(a, _))))

  def ~>[B](p: Field[B]): Field[B] = FieldF(doc => parent(doc).right flatMap (a => p(doc)))

  def <~[B](p: Field[B]): Field[A] = parent.~(p).map(_._1)

  def |[B >: A](p: Field[B]): Field[B] = FieldF(doc => parent(doc) fold (_ => p(doc), a => Right(a)))

  def opt: Field[Option[A]] = FieldF(doc => Right(parent(doc).right.toOption))

  def >>[B](f: A => Field[B]): Field[B] = flatMap(f)

  /* parsers in pattern matching, when errors are not relevant
   *
   * ```
   * val parser: DocParser[Int] = ...
   * collection.find().asScala collect {
   *   case parser(i) => i
   * }
   * ```
   */
  def unapply(doc: Any): Option[A] = apply(doc).right.toOption

  /**
   * optimistic parsing, e.g. we assuming we can parse everything we get. Otherwise
   * we'll get exception
   *
   * ```
   * collection.find().asScala map {parser.parse}
   * ```
   */
  def parse: Any => A =
    this(_) fold (msg => throw new Exception(msg), x => x)
}

case class FieldF[T](f: Any => ParseResult[T]) extends Field[T]{
    override def apply(o: Any) = f(o)
}

case class FieldPf[+T](pf: PartialFunction[Any, T]) extends Field[T] {
  override def apply(o: Any): ParseResult[T] = PartialFunction.condOpt(o)(pf).toRight("Can't parse %s" format o)

  def orElse[B1 >: T](pf2: PartialFunction[Any,B1]): FieldPf[B1] = copy(pf = pf orElse pf2)
  def orElse[B1 >: T](g: FieldPf[B1]): FieldPf[B1] = orElse(g.pf)

  def andThen[R](pf2: PartialFunction[T,R]) =
    copy(pf = new PartialFunction[Any,R] {
        override def isDefinedAt(x: Any) = pf.isDefinedAt(x) && pf2.isDefinedAt(pf(x))
        override def apply(x: Any): R = pf2(pf(x))
      })
}

object Field {
  import collection.JavaConverters._
  import org.bson.types.BasicBSONList

  def apply[T](pf: PartialFunction[Any,T]): FieldPf[T] = new FieldPf[T](pf)

  //
  // Default readers
  //

  implicit val booleanGetter = Field[Boolean]({ case b: Boolean => b })
  implicit val intGetter = Field[Int]({ case i: Int => i })
  implicit val shortGetter = Field[Short]({ case i: Short => i })
  implicit val longGetter = Field[Long]({ case l: Long => l })
  implicit val floatGetter = Field[Float]({
      case d: Double => d.floatValue
      case f: Float => f
    })
  implicit val doubleGetter = Field[Double]({ case d: Double => d })
  implicit val dateGetter = Field[Date]({ case d: Date => d })

  implicit val objIdGetter = Field[ObjectId]({ case objId: ObjectId => objId })
  implicit val dboGetter = Field[DBObject]({ case dbo: DBObject => dbo })
  implicit val patternGetter = Field[Pattern]({ case p: Pattern => p })
  implicit val stringGetter = Field[String]({
    case s: String => s
    case s: BsonSymbol => s.getSymbol
    case oid: ObjectId => oid.toStringMongod
  })
  implicit val symbolGetter = Field[Symbol]({
    case s: Symbol => s
    case s: BsonSymbol => Symbol(s.getSymbol)
  })
  implicit val regexGetter = Field[Regex]({
    case p: Pattern => new Regex(p.pattern)
    case r: Regex => r
  })

  implicit def byteArrayGetter = Field[Array[Byte]]({
      case b: Binary => b.getData
      case a: Array[Byte] => a
    })
  implicit def arrayGetter[T](implicit r: Field[T], m: Manifest[T]) = new Field[Array[T]] {
    override def apply(v: Any) = v match {
      case a: Array[_] if m.isInstanceOf[reflect.AnyValManifest[_]] && a.getClass == m.arrayManifest.runtimeClass => Right(a.asInstanceOf[Array[T]])
      case a: Array[_] => allOrNone(a map (r.apply _)).right.map(_.toArray)
      case list: BasicBSONList => allOrNone(list.asScala map (r.apply _)).right.map(_.toArray)

    }
  }

  implicit def optionGetter[T](implicit r: Field[T]) =
    new Field[Option[T]] {
      override def apply(o: Any): ParseResult[Option[T]] = Right(r.apply(o).right.toOption)
    }

  implicit def listGetter[T](implicit r: Field[T]) = new Field[List[T]] {
    override def apply(v: Any) = v match {
      case ar: Array[_] => allOrNone(ar.map(r.apply _)).right.map(_.toList)
      case list: BasicBSONList => allOrNone(list.asScala map (r.apply _)).right.map(_.toList)
    }
  }

  implicit def tuple2Getter[T1,T2](implicit r1: Field[T1], r2: Field[T2]) =
    new Field[Tuple2[T1,T2]] {
      override def apply(o: Any): ParseResult[Tuple2[T1,T2]] =
        o match {
          case a: Array[_] if a.size == 2 =>
            for {
              v1 <- r1(a(0)).right
              v2 <- r2(a(1)).right
            } yield (v1, v2)
        }
    }

  def allOrNone[L, R](results: Seq[Either[L, R]]): Either[L, List[R]] =
    results collectFirst {
      case Left(msg) => Left(msg)
    } getOrElse Right(results.map(_.right.get).toList)

  // TODO: Field[Map[String,T]]

}

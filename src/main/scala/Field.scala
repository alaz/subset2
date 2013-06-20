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

import BsonParser.ParseResult

@implicitNotFound(msg = "Cannot find Field for ${A}")
trait Field[+A] { parent =>
  def apply(o: Any): ParseResult[A]

  def map[B](f: A => B): Field[B] = new Field[B] {
    override def apply(o: Any) = parent.apply(o).right map f
  }
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
    override def apply(o: Any) = o match {
      case a: Array[_] => mergeResults(a.map(r.apply _)).right.map(_.toArray)
      case list: BasicBSONList => mergeResults(list.asScala.map(r.apply _)).right.map(_.toArray)
    }
  }

  implicit def optionGetter[T](implicit r: Field[T]) =
    new Field[Option[T]] {
      override def apply(o: Any): ParseResult[Option[T]] = r.apply(o).right map Some.apply
    }

  implicit def listGetter[T](implicit r: Field[T]) = new Field[List[T]] {
    override def apply(o: Any) = o match {
      case ar: Array[_] => mergeResults(ar.map(r.apply))
      case list: BasicBSONList => mergeResults(list.asScala.map(r.apply))
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
  // TODO: Field[Map[String,T]]

  def mergeResults[L, R](results: Seq[Either[L, R]]): Either[L, List[R]] = {
    @annotation.tailrec
    def helper(eithers: Seq[Either[L, R]], acc: List[R]): Either[L, List[R]] = eithers match {
      case Left(err) :: xs => Left(err)
      case Right(x) :: xs => helper(xs, x :: acc)
      case _ => Right(acc.reverse)
    }
    helper(results, Nil)
  }
}

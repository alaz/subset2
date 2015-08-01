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

import org.bson.BasicBSONObject

import annotation.implicitNotFound
import java.util.Date
import java.util.regex.Pattern
import util.matching.Regex
import org.bson.types.{ObjectId, Binary, Symbol => BsonSymbol}
import com.mongodb.DBObject

@implicitNotFound(msg = "Cannot find Field for ${A}")
trait Field[+A] { parent =>
  def apply(o: Any): Option[A]

  def map[B](f: A => B): Field[B] = new Field[B] {
    override def apply(o: Any) = parent.apply(o) map f
  }
}

case class FieldPf[+T](pf: PartialFunction[Any, T]) extends Field[T] {
  override def apply(o: Any): Option[T] = PartialFunction.condOpt(o)(pf)

  def orElse[B1 >: T](pf2: PartialFunction[Any,B1]): FieldPf[B1] = copy(pf = pf orElse pf2)
  def orElse[B1 >: T](g: FieldPf[B1]): FieldPf[B1] = orElse(g.pf)

  def andThen[R](pf2: PartialFunction[T,R]) =
    copy(pf = new PartialFunction[Any,R] {
        override def isDefinedAt(x: Any) = pf.isDefinedAt(x) && pf2.isDefinedAt(pf(x))
        override def apply(x: Any): R = pf2(pf(x))
      })
}

object Field {
  import collection.convert.decorateAsScala._
  import org.bson.types.BasicBSONList

  def apply[T](pf: PartialFunction[Any,T]): FieldPf[T] = new FieldPf[T](pf)

  def read[T](o: Any)(implicit f: Field[T]): Option[T] = f(o)

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
    case oid: ObjectId => oid.toHexString
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
      case a: Array[_] if m.isInstanceOf[reflect.AnyValManifest[_]] && a.getClass == m.arrayManifest.runtimeClass => Some(a.asInstanceOf[Array[T]])
      case a: Array[_] => allOrNone(a map (Field.read[T])) map(_.toArray)
      case list: BasicBSONList => allOrNone(list.asScala map (Field.read[T])) map(_.toArray)
    }
  }

  implicit def optionGetter[T](implicit r: Field[T]) =
    new Field[Option[T]] {
      override def apply(o: Any): Option[Option[T]] = Some(r.apply(o))
    }
  implicit def listGetter[T](implicit r: Field[T]) = new Field[List[T]] {
    override def apply(v: Any) = v match {
      case ar: Array[_] => allOrNone(ar.map(Field.read[T]).toList) map(_.toList)
      case list: BasicBSONList => allOrNone(list.asScala map (Field.read[T])) map(_.toList)
    }
  }
  implicit def tuple2Getter[T1,T2](implicit r1: Field[T1], r2: Field[T2]) =
    new Field[Tuple2[T1,T2]] {
      def maybeTuple(seq: Seq[_]) =
        for {v1 <- Field.read[T1](seq(0)); v2 <- Field.read[T2](seq(1))}
        yield (v1, v2)

      override def apply(o: Any): Option[Tuple2[T1,T2]] =
        o match {
          case list: BasicBSONList if list.size == 2 => maybeTuple(list.asScala)
          case a: Array[_] if a.size == 2 => maybeTuple(a)
        }
    }

  implicit def mapGetter[T](implicit r: Field[T]) = new Field[Map[String,T]] {
    override def apply(v: Any) = v match {
      case dbo: BasicBSONObject =>
        allOrNone(
          dbo.asScala.map { case (key, value) => r(value).map(key -> _) }
        ).map(_.toMap)
      case _ =>
        None
    }
  }

  implicit def fromParser[T](implicit p: DocParser[T]) = Field[T]({ case p(v) => v })

  def allOrNone[T](results: Traversable[Option[T]]): Option[Traversable[T]] =
    if (results exists (_.isEmpty))
      None
    else
      Some(results.flatten)
}

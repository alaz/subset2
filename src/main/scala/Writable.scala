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

@implicitNotFound(msg = "Cannot find BsonWritable for ${A}")
trait BsonWritable[-A] {
  def apply(a: A): Option[Any]
}

object BsonWritable {
  def apply[T](sane: (T => Any)): BsonWritable[T] =
    new BsonWritable[T] {
      override def apply(x: T): Option[Any] = Some(sane(x))
    }

  //
  // Default writers
  //
  val anyWriter = BsonWritable[Any](identity _)
  implicit val booleanSetter = BsonWritable[Boolean](identity _)
  implicit val intSetter = BsonWritable[Int](identity _)
  implicit val shortSetter = BsonWritable[Short](identity _)
  implicit val longSetter = BsonWritable[Long](identity _)
  implicit val floatSetter = BsonWritable[Float](identity _)
  implicit val doubleSetter = BsonWritable[Double](identity _)
  implicit val dateSetter = BsonWritable[Date](identity _)
  implicit val objIdSetter = BsonWritable[ObjectId](identity _)
  implicit val dboSetter = BsonWritable[DBObject](identity _)
  implicit val patternSetter = BsonWritable[Pattern](identity _)
  implicit val stringSetter = BsonWritable[String](identity _)

  implicit val symbolSetter = BsonWritable[Symbol](s => new BsonSymbol(s.name))
  implicit val regexSetter = BsonWritable[Regex](r => r.pattern)

  implicit val byteArraySetter = BsonWritable[Array[Byte]](new Binary(_))
  implicit val arraySetter = BsonWritable[Array[_]](identity _)

  implicit def optionSetter[T](implicit w: BsonWritable[T]) =
    new BsonWritable[Option[T]] {
      override def apply(x: Option[T]): Option[Any] = x.flatMap(w.apply _)
    }
  implicit def seqSetter[T](implicit w: BsonWritable[T]) =
    new BsonWritable[Traversable[T]] {
      override def apply(x: Traversable[T]): Option[Any] = Some( x.flatMap(w.apply _).toArray )
    }
  implicit def tuple2Setter[T1,T2](implicit w1: BsonWritable[T1], w2: BsonWritable[T2]) =
    new BsonWritable[Tuple2[T1,T2]] {
      override def apply(t: Tuple2[T1,T2]) =
        for {x1 <- w1.apply(t._1); x2 <- w2.apply(t._2)}
        yield Array(x1,x2)
    }
  // TODO: BsonWritable[Map[String,T]]
}

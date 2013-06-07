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

import java.util.Date
import org.bson.types.ObjectId

object SmartFields extends SmartFields

trait SmartFields {
  import Extractors._
  import Field._

  implicit val objIdRecoveringGetter = Field[ObjectId]({
      case objId: ObjectId => objId
    }) orElse stringGetter.andThen({ case AsObjectId(oid) => oid })
  implicit val stringRecoveringGetter =
    stringGetter orElse Field[String]({
      case i: Int => i.toString
      case l: Long => l.toString
      case b: Boolean => b.toString
    })
  implicit val booleanRecoveringGetter = Field[Boolean]({
      case b: Boolean => b
      case i: Int => i != 0
      case l: Long => l != 0
      case f: Float => f != 0.0
      case d: Double => d != 0.0
      case s: String => !s.isEmpty
  })
  implicit val shortRecoveringGetter = Field[Short]({
      case i: Int => i.shortValue
      case l: Long => l.shortValue
    }) orElse stringGetter.andThen({ case AsInt(i) => i.shortValue })

  implicit val intRecoveringGetter = Field[Int]({
      case i: Int => i
      case l: Long => l.intValue
    }) orElse stringGetter.andThen({ case AsInt(i) => i })

  implicit val longRecoveringGetter = Field[Long]({
      case i: Int => i.longValue
      case l: Long => l
    }) orElse stringGetter.andThen({ case AsLong(l) => l })

  implicit val byteRecoveringGetter = Field[Byte]({
      case i: Int => i.byteValue
      case l: Long => l.byteValue
    }) orElse stringGetter.andThen({ case AsInt(i) => i.byteValue })

  implicit val doubleRecoveringGetter = Field[Double]({
      case i: Int => i.doubleValue
      case l: Long => l.doubleValue
      case d: Double => d
    }) orElse stringGetter.andThen({ case AsDouble(d) => d })
  implicit val floatRecoveringGetter = Field[Float]({
      case i: Int => i.floatValue
      case l: Long => l.floatValue
      case d: Double => d.floatValue
    }) orElse stringGetter.andThen({ case AsDouble(d) => d.floatValue })

  implicit val dateRecoveringGetter = Field[Date]({
      case d: Date => d
      case i: Int => new Date(i*1000L)
      case l: Long => new Date(l)
    })
}


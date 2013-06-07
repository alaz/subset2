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

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class smartFieldSpec extends FunSpec with ShouldMatchers with MongoMatchers with Routines {
  import org.bson.types.{ObjectId, Symbol => BsonSymbol}
  val explicit = SmartFields

  describe("SmartFields") {
    it("should get") {
      import explicit._
      unpack[String]("string") should equal(Some("string"))
      unpack[Symbol](new BsonSymbol("Sym")) should equal(Some('Sym))
      unpack[Symbol]("sym") should equal(None)
      unpack[String]('Sym) should equal(None)
    }
    it("recovers ObjectId") {
      unpack[ObjectId](11)(explicit.objIdRecoveringGetter) should equal(None)
      unpack[ObjectId]("11")(explicit.objIdRecoveringGetter) should equal(None)
      unpack[ObjectId](new ObjectId)(explicit.objIdRecoveringGetter) should be('defined)
      unpack[ObjectId]( (new ObjectId).toString )(explicit.objIdRecoveringGetter) should be('defined)
    }
    it("recovers Int") {
      unpack[Int](11)(explicit.intRecoveringGetter) should equal(Some(11))
      unpack[Int]("10")(explicit.intRecoveringGetter) should equal(Some(10))
      unpack[Int](109L)(explicit.intRecoveringGetter) should equal(Some(109))
      unpack[Int]("x13")(explicit.intRecoveringGetter) should equal(None)
    }
    it("recovers Long") {
      unpack[Long](11)(explicit.longRecoveringGetter) should equal(Some(11L))
      unpack[Long]("10")(explicit.longRecoveringGetter) should equal(Some(10L))
      unpack[Long](109L)(explicit.longRecoveringGetter) should equal(Some(109L))
      unpack[Long]("x13")(explicit.longRecoveringGetter) should equal(None)
    }
    it("recovers Double") {
      unpack[Double](11.76)(explicit.doubleRecoveringGetter) should equal(Some(11.76))
      unpack[Double](67)(explicit.doubleRecoveringGetter) should equal(Some(67.0))
      unpack[Double]("10.87")(explicit.doubleRecoveringGetter) should equal(Some(10.87))
      unpack[Double](109L)(explicit.doubleRecoveringGetter) should equal(Some(109.0))
      unpack[Double]("x13")(explicit.doubleRecoveringGetter) should equal(None)
    }
    it("recovers Float") {
      unpack[Float](67)(explicit.floatRecoveringGetter) should equal(Some(67.0F))
      unpack[Float]("10.87")(explicit.floatRecoveringGetter) should equal(Some(10.87F))
      unpack[Float](109L)(explicit.floatRecoveringGetter) should equal(Some(109.0F))
      unpack[Float]("x13")(explicit.floatRecoveringGetter) should equal(None)
    }
    it("recovers Boolean") {
      unpack[Boolean](67)(explicit.booleanRecoveringGetter) should equal(Some(true))
      unpack[Boolean]("")(explicit.booleanRecoveringGetter) should equal(Some(false))
      unpack[Boolean]("0")(explicit.booleanRecoveringGetter) should equal(Some(true))
      unpack[Boolean](0.0)(explicit.booleanRecoveringGetter) should equal(Some(false))
      unpack[Boolean](0.3)(explicit.booleanRecoveringGetter) should equal(Some(true))
      unpack[Boolean](0L)(explicit.booleanRecoveringGetter) should equal(Some(false))
    }
    it("recovers Date") {
      import java.util.Date
      // round to seconds
      val now = {
        val d = new Date
        d.setTime(d.getTime/1000 * 1000L)
        d
      }

      unpack[Date](now)(explicit.dateRecoveringGetter) should equal(Some(now))
      unpack[Date]((now.getTime/1000).intValue)(explicit.dateRecoveringGetter) should equal(Some(now))
      unpack[Date](now.getTime)(explicit.dateRecoveringGetter) should equal(Some(now))
    }

  }
}

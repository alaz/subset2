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

import org.scalatest.{FunSpec,Matchers,OptionValues}

class writableSpec extends FunSpec with Matchers with MongoMatchers with OptionValues with Routines {
  describe("Base primitives serializer") {
    import org.bson.types.{Symbol => BsonSymbol}

    it("should set explicitly") {
      pack("val")(BsonWritable.stringSetter) should equal(Some("val"))
      val sym = pack('Sym)(BsonWritable.symbolSetter)
      sym should equal(Some(new BsonSymbol("Sym")))
    }
  }
  describe("Option writer") {
    it("sets Some") {
      pack[Option[Int]](Some(1)) should equal(Some(1))
    }
    it("sets None") {
      pack[Option[Int]](None: Option[Int]) should be('empty)
    }
  }
  describe("List writer") {
    it("sets empty List[T]") {
      pack(Nil: List[Int]).value should equal(Array[Int]())
    }
    it("sets non-empty List[T]") {
      pack(List(1,2)).value should equal(Array(1,2))
    }
    it("sets Iterable[T]") {
      pack(Iterable(1,2)).value should equal(Array(1,2))
    }
  }
  describe("Tuple writer") {
    it("sets Tuple2") {
      pack("s" -> 10).map{_.asInstanceOf[Array[_]].toList} should equal(Some(Array("s", 10).toList))
    }
    it("gets Tuple2") {
      unpack[Tuple2[String,Int]](Array("s", 10)) should equal(Some("s" -> 10))
    }
  }
  describe("Writer for recursive structures") {
    it("is possible") {
      DBO("r" -> Rec(123, None))() should containField("r")
      DBO("r" -> Rec(123, Some(Rec(234,None) :: Nil)))() should containField("r")
    }
  }
}

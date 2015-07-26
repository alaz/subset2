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

import java.util

import org.bson.BasicBSONObject
import org.bson.types.BasicBSONList
import org.scalatest.{FunSpec,Matchers,OptionValues}

import com.mongodb.BasicDBList

class fieldSpec extends FunSpec with Matchers with MongoMatchers with OptionValues with Routines {
  describe("Option reader") {
    it("never returns None") {
      unpack[Option[Int]](1) should equal(Some(Some(1)))
      unpack[Option[Int]]("str") should equal(Some(None))
    }
  }
  describe("List reader") {
    it("gets List[T] from BSON DBList") {
      val da = new BasicDBList
      da.put(0, 1)
      da.put(1, 2)
      da.put(2, 3)
      unpack[List[Int]](da) should equal(Some(List(1,2,3)))
    }
    it("gets List[T] from Array") {
      unpack[List[Int]](Array(1,2,3)) should equal(Some(List(1,2,3)))
    }
  }
  describe("Array reader") {
    it("gets Array[T] from BSON DBList") {
      val da = new BasicDBList
      da.put(0, 1)
      da.put(1, 2)
      da.put(2, 3)
      val opt = unpack[Array[Int]](da)
      opt should be('defined)
      opt.get should equal(Array(1,2,3))
    }
    it("gets Array[T] from Array") {
      val opt = unpack[Array[Int]](Array(1,2,3))
      opt.value should equal(Array(1,2,3))
    }
    it("must use Field.apply on array items") {
      case class F(i: Int)
      implicit val field = Field[F] { case i: Int => F(i) }
      val opt = unpack[Array[F]](Array(1,2))
      opt.value should equal(Array(F(1), F(2)))
    }
    it("must return array of matching primitive types as is") {
      val arr = Array(1, 2)
      unpack[Array[Int]](arr).value should (be theSameInstanceAs arr)
    }
    it("must convert array of not-matching primitive types") {
      import SmartFields.doubleRecoveringGetter
      val arr = Array(1, 2)
      val opt = unpack[Array[Double]](arr)
      opt.value should (not (be theSameInstanceAs arr) and equal(Array(1.0, 2.0)))
    }
    it("must return None if at least one item conversion failed") {
      val arr = Array(1, 2.0)
      val opt = unpack[Array[Int]](arr)
      opt should be('empty)
    }
  }
  describe("Tuple reader") {
    it("must read from Array") {
      val arr = Array(2, "str")
      val opt = unpack[(Int,String)](arr)
      opt should equal(Some(2 -> "str"))
    }
    it("must read from BSONList") {
      val da = new BasicDBList
      da.put(0, 2)
      da.put(1, "str")
      val opt = unpack[(Int,String)](da)
      opt should equal(Some(2 -> "str"))
    }
  }
  describe("Map reader with String key") {
    it("must read from BasicBSONObject") {
      val da = new BasicBSONObject("name1", 1L)
      da.append("name2", 2L)
//      val opt = unpack[scala.collection.Map[String, Long]](da)
//      opt should equal(Some(Map("name1" -> 1L, "name2" -> 2L)))
    }
  }
//  describe("Map reader with any key") {
//    it("must read from BasicBSONList") {
//      val da = new BasicBSONList()
//
//      val firstItem = new BasicBSONList()
//      firstItem.put(0, 10L)
//      firstItem.put(1, "value10")
//      da.put(0, firstItem)
//
//      val secondItem = new BasicBSONList()
//      secondItem.put(0, 20L)
//      secondItem.put(1, "value20")
//      da.put(1, secondItem)
//
//      val opt = unpack[Map[Long, String]](da)
//      opt should equal(Some(Map(10L -> "value10", 20L -> "value20")))
//    }
//  }
  describe("Field") {
    it("can be mapped") {
      val field = Field.intGetter map (_.toString)
      field(2) should equal(Some("2"))
    }
  }
  describe("allOrNone") {
    it("converts sequence of Some into Some(sequence)") {
      Field.allOrNone(Seq(Some(1), Some(2))) should equal(Some(List(1, 2)))
    }
    it("converts empty sequence into Some(Nil)") {
      Field.allOrNone(Seq()) should equal(Some(Nil))
    }
    it("returns None if any item is None") {
      Field.allOrNone(Seq(Some(1), None)) should equal(None)
    }
  }
}

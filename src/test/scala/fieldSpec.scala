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

import com.mongodb.BasicDBList

class fieldSpec extends FunSpec with ShouldMatchers with MongoMatchers with Routines {
  describe("Option reader") {
    it("never returns None") {
      unpack[Option[Int]](1) should equal(Right(Some(1)))
      unpack[Option[Int]]("str") should equal(Right(None))
    }
  }
  describe("List reader") {
    it("gets List[T] from BSON DBList") {
      val da = new BasicDBList
      da.put(0, 1)
      da.put(1, 2)
      da.put(2, 3)
      unpack[List[Int]](da) should equal(Right(List(1,2,3)))
    }
    it("gets List[T] from Array") {
      unpack[List[Int]](Array(1,2,3)) should equal(Right(List(1,2,3)))
    }
  }
  describe("Array reader") {
    it("gets Array[T] from BSON DBList") {
      val da = new BasicDBList
      da.put(0, 1)
      da.put(1, 2)
      da.put(2, 3)
      val opt = unpack[Array[Int]](da)
      opt should be('right)
      opt.right.get should equal(Array(1,2,3))
    }
    it("gets Array[T] from Array") {
      val opt = unpack[Array[Int]](Array(1,2,3))
      opt should be('right)
      opt.right.get should equal(Array(1,2,3))
    }
    it("must use Field.apply on array items") {
      case class F(i: Int)
      implicit val field = Field[F] { case i: Int => F(i) }
      val opt = unpack[Array[F]](Array(1,2))
      opt should be('right)
      opt.right.get should equal(Array(F(1), F(2)))
    }
    it("must return array of matching primitive types as is") {
      val arr = Array(1, 2)
      val opt = unpack[Array[Int]](arr)
      opt should be('right)
      opt.right.get should (be theSameInstanceAs arr)
    }
    it("must convert array of not-matching primitive types") {
      import SmartFields.doubleRecoveringGetter
      val arr = Array(1, 2)
      val opt = unpack[Array[Double]](arr)
      opt should be('right)
      opt.right.get should not (be theSameInstanceAs arr)
      opt.right.get should equal(Array(1.0, 2.0))
    }
    it("must return None if at least one item conversion failed") {
      val arr = Array(1, 2.0)
      val opt = unpack[Array[Int]](arr)
      opt should be('left)
    }
  }
  describe("Field") {
    it("can be mapped") {
      val field = Field.intGetter map (_.toString)
      field(2) should equal(Right("2"))
    }
  }
  describe("allOrNone") {
    it("converts sequence of Right into Right(sequence)") {
      Field.allOrNone(Seq(Right(1), Right(2))) should equal(Right(List(1, 2)))
    }
    it("converts empty sequence into Right(Nil)") {
      Field.allOrNone(Seq()) should equal(Right(Nil))
    }
    it("returns Left if any item is Left") {
      Field.allOrNone(Seq(Right(1), Left("msg"))) should equal(Left("msg"))
    }
  }
}

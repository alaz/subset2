package com.osinka.subset

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import com.mongodb.BasicDBList

class fieldSpec extends FunSpec with ShouldMatchers with MongoMatchers with Routines {
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
      opt should be('defined)
      opt.get should equal(Array(1,2,3))
    }
  }
}
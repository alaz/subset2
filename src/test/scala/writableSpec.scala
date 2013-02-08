package com.osinka.subset

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class writableSpec extends FunSpec with ShouldMatchers with MongoMatchers with Routines {
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
      val o = pack(Nil: List[Int])
      o should be('defined)
      o.get should equal(Array[Int]())
    }
    it("sets non-empty List[T]") {
      val o = pack(List(1,2))
      o should be('defined)
      o.get should equal(Array(1,2))
    }
    it("sets Iterable[T]") {
      val o = pack(Iterable(1,2))
      o should be('defined)
      o.get should equal(Array(1,2))
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
}

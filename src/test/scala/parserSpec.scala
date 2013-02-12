package com.osinka.subset

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import com.mongodb.BasicDBObjectBuilder.{start => dbo}
import org.bson.types.{ObjectId, Symbol => BsonSym}

class parserSpec extends FunSpec with ShouldMatchers with MongoMatchers with Routines {
  import DocParser._

  describe("Basic parser") {
    it("retrieves a field") {
      get[Int]("a").apply(dbo("a", 10).get) should equal(Right(10))
      get[String]("a").apply(dbo("a", "bb").get) should equal(Right("bb"))
    }
    it("fails to retrieve wrong field type") {
      get[Int]("a").apply(dbo("a", "str").get) should be('left)
    }
    it("retrieves by path") {
      get[Int]("a.b" split "\\.").apply(dbo.push("a").add("b", 10).get) should equal(Right(10))
    }
    it("checks for existence") {
      contains[ObjectId]("id", new ObjectId).apply(dbo("id", new ObjectId).get) should be('left)
      contains[Int]("id", 10).apply(dbo("id", "str").get) should be('left)
      contains[Int]("id", 10).apply(dbo("id", 10).get) should be('right)
    }
  }
  describe("Combinator") {
    it("converts to optional") {
      val parser: DocParser[Option[Int]] = int("a").opt

      parser.apply(dbo("a", 10).get) should equal(Right(Some(10)))
      parser.apply(dbo.get) should equal(Right(None))
    }
    it("descends into subdocuments") {
      val parser = doc("a")(int("b") ~ str("c"))
      val matching = dbo.push("a").add("b", 5).add("c", "str").get
      parser.apply(matching) should be('right)

      val tupled = parser map { case i ~ s => (i,s) }
      tupled(matching) should equal(Right(5 -> "str"))
    }
    it("selects") {
      val parser = str("event") >> {
        case "ourtype" =>
          (contains("version", 1) ~> int("key") map { (_: Int).toLong }) |
           contains("version", 2) ~> long("l")
        case eventId =>
          fails("wrong event %s" format eventId)
      }

      // the snippet above could be expressed more idiomatic as
      // contains("event", "ourtype") ~> (
      //   contains("version", 1) ~> int("key") |
      //   contains("version", 2) ~> long("l)
      // )

      parser(dbo.get) should be('left)
      parser(dbo("version", 1).add("key", 10).get) should be('left)
      parser(dbo("event", "not_our").add("version", 1).add("key", 10).get) should be('left)
      parser(dbo("event", "ourtype").add("version", 1).add("key", 10).get) should equal(Right(10))
      parser(dbo("event", "ourtype").add("version", 2).add("l", 10L).get) should equal(Right(10))
    }
    it("selects with greater tolerance") {
      import SmartFields._
      val parser = str("event") >> {
        case "ourtype" =>
          contains("version", 1) ~> int("key") | contains("version", 2) ~> long("l")
        case eventId =>
          fails("wrong event %s" format eventId)
      }

      parser(dbo("event", new BsonSym("ourtype")).add("version", 1).add("key", 10L).get) should be('right)
      parser(dbo("event", new BsonSym("ourtype")).add("version", 2).add("l", 10).get) should be('right)
    }
  }
}

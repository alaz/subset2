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

import org.scalatest.{FunSpec,Matchers}

import com.mongodb.BasicDBObjectBuilder.{start => dbo}
import org.bson.types.{ObjectId, Symbol => BsonSym}
import org.bson.Document

class parserSpec extends FunSpec with Matchers with MongoMatchers with Routines {
  import DocParser._

  describe("Basic parser") {
    it("retrieves a field") {
      get[Int]("a").apply(dbo("a", 10).get) should equal(Right(10))
      get[Int]("a").apply(new Document("a", 10)) should equal(Right(10))

      get[String]("a").apply(dbo("a", "bb").get) should equal(Right("bb"))
      get[String]("a").apply(new Document("a", "bb")) should equal(Right("bb"))
    }
    it("fails to retrieve wrong field type") {
      get[Int]("a").apply(dbo("a", "str").get) should be('left)
      get[Int]("a").apply(new Document("a", "str")) should be('left)
    }
    it("retrieves by path") {
      get[Int]("a.b" split "\\.").apply(
        dbo.push("a").add("b", 10).get
      ) should equal(Right(10))
      get[Int]("a.b" split "\\.").apply(
        new Document().append("a", new Document("b", 10))
      ) should equal(Right(10))
    }
    it("checks for existence") {
      contains[ObjectId]("id", new ObjectId).apply(dbo("id", new ObjectId).get) should be('left)
      contains[ObjectId]("id", new ObjectId).apply(new Document("id", new ObjectId)) should be('left)

      contains[Int]("id", 10).apply(dbo("id", "str").get) should be('left)
      contains[Int]("id", 10).apply(new Document("id", "str")) should be('left)

      contains[Int]("id", 10).apply(dbo("id", 10).get) should be('right)
      contains[Int]("id", 10).apply(new Document("id", 10)) should be('right)
    }
  }
  describe("Combinator") {
    it("converts to optional") {
      val parser: DocParser[Option[Int]] = int("a").opt

      parser.apply(dbo("a", 10).get) should equal(Right(Some(10)))
      parser.apply(new Document("a", 10)) should equal(Right(Some(10)))

      parser.apply(dbo.get) should equal(Right(None))
      parser.apply(new Document) should equal(Right(None))
    }
    it("descends into subdocuments") {
      val parser = doc("a")(int("b") ~ str("c"))
      val matchingDbo = dbo.push("a").add("b", 5).add("c", "str").get
      val matchingDoc = new Document("a", new Document("b", 5).append("c", "str"))

      parser.apply(matchingDbo) should be('right)
      parser.apply(matchingDoc) should be('right)

      val tupled = parser map { case i ~ s => (i,s) }
      tupled(matchingDbo) should equal(Right(5 -> "str"))
      tupled(matchingDoc) should equal(Right(5 -> "str"))
    }
    it("selects") {
      val parser = str("event") >> {
        case "ourtype" =>
          (contains("version", 1) ~> int("key") map { (_: Int).toLong }) |
           contains("version", 2) ~> long("l")
        case eventId =>
          fails(s"wrong event $eventId")
      }

      // the snippet above could be expressed more idiomatic as
      // contains("event", "ourtype") ~> (
      //   contains("version", 1) ~> int("key") |
      //   contains("version", 2) ~> long("l)
      // )

      parser(dbo.get) should be('left)
      parser(new Document) should be('left)

      parser(dbo("version", 1).add("key", 10).get) should be('left)
      parser(new Document("version", 1).append("key", 10)) should be('left)

      parser(dbo("event", "not_our").add("version", 1).add("key", 10).get) should be('left)
      parser(new Document("event", "not_our").append("version", 1).append("key", 10)) should be('left)

      parser(dbo("event", "ourtype").add("version", 1).add("key", 10).get) should equal(Right(10))
      parser(new Document("event", "ourtype").append("version", 1).append("key", 10)) should equal(Right(10))

      parser(dbo("event", "ourtype").add("version", 2).add("l", 10L).get) should equal(Right(10))
      parser(new Document("event", "ourtype").append("version", 2).append("l", 10L)) should equal(Right(10))
    }
    it("selects with greater tolerance") {
      import SmartFields._
      val parser = str("event") >> {
        case "ourtype" =>
          contains("version", 1) ~> int("key") | contains("version", 2) ~> long("l")
        case eventId =>
          fails(s"wrong event $eventId")
      }

      parser(dbo("event", new BsonSym("ourtype")).add("version", 1).add("key", 10L).get) should be('right)
      parser(new Document("event", new BsonSym("ourtype")).append("version", 1).append("key", 10L)) should be('right)

      parser(dbo("event", new BsonSym("ourtype")).add("version", 2).add("l", 10).get) should be('right)
      parser(new Document("event", new BsonSym("ourtype")).append("version", 2).append("l", 10)) should be('right)
    }
  }
  describe("Parser for recursive structures") {
    it("is possible") {
      Rec.Doc( DBO("id" -> 123)() ) should equal(Right(Rec(123, None)))
      Rec.Doc( new Document("id", 123) ) should equal(Right(Rec(123, None)))
    }
    it("can be used as another field") {
      val y = DocParser.get[Rec]("y")
      val r = Rec(123, Some(Rec(234, None) :: Rec(345, Some(Rec(456, None) :: Nil)) :: Nil))
      val dbo = DBO("y" -> r)()
      y(dbo) should equal(Right(r))
    }
  }
}

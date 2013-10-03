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

class optParserSpec extends FunSpec with Matchers with MongoMatchers with Routines {
  import DocParser._

  describe("Optional parser") {
    val parser: DocParser[Option[Int]] = int("a").opt

    it("returns Some if parsing succeeds") {
      parser.apply(dbo("a", 10).get) should equal(Right(Some(10)))
    }
    it("returns None if parsing fails") {
      parser.apply(dbo("a", "b").get) should equal(Right(None))
    }
  }

  describe("Parser of Option") {
    val parser: DocParser[Option[Int]] = get[Option[Int]]("a")

    it("returns Some if succeeds") {
      parser.apply(dbo("a", 10).get) should equal(Right(Some(10)))
    }
    it("returns None if cannot decode") {
      // TODO: we could fail if we had Field error reporting
      // see https://github.com/osinka/subset2/issues/7
      // see https://github.com/osinka/subset2/issues/10
      parser.apply(dbo("a", "b").get) should equal(Right(None))
    }
    it("fails if no field exists") {
      parser.apply(dbo.get) should be('left)
    }
  }

  describe("Parser of optional field, failing on wrong type") {
    val parser: DocParser[Option[Int]] =
      getRaw("a").opt >> {
        case Some(_) => get[Int]("a") map {Some(_)}
        case None => success(None)
      }

    // alternative:
    //val parser: DocParser[Option[Int]] = (get[Int]("a") map {Some(_)}) | (getRaw("a").opt.collect("msg") { case None => None })

    it("returns Some if succeeds") {
      parser.apply(dbo("a", 10).get) should equal(Right(Some(10)))
    }
    it("fails if cannot decode") {
      parser.apply(dbo("a", "b").get) should be('left)
    }
    it("returns None if no field exists") {
      parser.apply(dbo.get) should equal(Right(None))
    }
  }
}
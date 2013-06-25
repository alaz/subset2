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

/*
 * Recursive data structure
 */
case class Rec(id: Int, children: Option[List[Rec]])

object Rec {
  // writer
  implicit object asBson extends BsonWritable[Rec] {
    override def apply(rec: Rec) =
      Some( DBO("id" -> rec.id, "children" -> rec.children)() )
  }

  // reader, variant 1
  /*
  import DocParser._

  implicit lazy val recField = Field({ case Doc(rec) => rec })
  lazy val Doc: DocParser[Rec] =
    get[Int]("id") ~ get[List[Rec]]("children").opt map {
      case id ~ children => new Rec(id, children)
    }
  */

  // reader, variant 2
  implicit object recField extends Field[Rec] {
    import DocParser._

    lazy val Doc: Field[Rec] =
      get[Int]("id") ~ get[List[Rec]]("children").opt map {
        case id ~ children => new Rec(id, children)
      }

    override def apply(o: Any) = Doc(o)
  }
  def Doc = recField.Doc // so that we don't need to change the spec
}

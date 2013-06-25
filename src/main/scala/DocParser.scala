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

import org.bson.types.ObjectId
import com.mongodb.DBObject

object DocParser {
  def get[T : Field](path: Seq[String]): Field[T] =
    path match {
      case Seq(name) => get[T](name)
      case _ => doc[T](path.head)(get[T](path.tail))
    }

  def get[T](name: String)(implicit f: Field[T]): Field[T] =
    FieldF[T] { _ match {
      case o: DBObject => Option(o.get(name)) map f.apply getOrElse Left("No field `"+name+"`")
      case _ => Left("Not object")
    }}

  def doc[T](name: String)(parser: Field[T])(implicit f: Field[DBObject]): Field[T] =
    get[DBObject](name).flatMap(doc => FieldF[T](_ => parser(doc)))

  def int(name: String)(implicit f: Field[Int]): Field[Int] = get[Int](name)

  def str(name: String)(implicit f: Field[String]): Field[String] = get[String](name)

  def bool(name: String)(implicit f: Field[Boolean]): Field[Boolean] = get[Boolean](name)

  def long(name: String)(implicit f: Field[Long]): Field[Long] = get[Long](name)

  def sym(name: String)(implicit f: Field[Symbol]): Field[Symbol] = get[Symbol](name)

  def oid(name: String)(implicit f: Field[ObjectId]): Field[ObjectId] = get[ObjectId](name)

  def contains[T : Field](name: String, t: T): Field[Unit] =
    get[T](name).collect("No field `"+name+"` with value `"+t+"`") { case a if a == t => Unit }

  def fails(msg: String) = FieldF[Unit](_ => Left(msg))

  def docId(implicit f: Field[ObjectId]): Field[ObjectId] = oid("_id")
}

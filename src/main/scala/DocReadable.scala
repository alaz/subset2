/**
 * Copyright (C) 2015 Alexander Azarov <azarov@osinka.com>
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

import org.bson.{Document => MongoDocument, BSONObject}
import com.mongodb.DBObject

trait DocReadable extends BSONObject


class DocumentReadable(doc: MongoDocument) extends DocReadable {
  override def containsField(s: String) = doc.containsField(s)
  override def containsKey(key: String) = doc.containsKey(key)
  override def get(key: String): AnyRef = doc.get(key)
  override def keySet() = doc.keySet()
  override def put(s: String, v: Any) = doc.put(s, v)
  override def putAll(o: BSONObject): Unit = { doc.putAll(o) }
  override def putAll(m: java.util.Map[_,_]): Unit = { doc.putAll(m) }
  override def removeField(key: String) = doc.removeField(key)
  override def toMap = doc.toMap
}

trait DocReadables {
  import scala.language.implicitConversions

  implicit def decorateDocument(doc: MongoDocument): DocReadable = new DocumentReadable(doc)
}

object DocReadables extends DocReadables
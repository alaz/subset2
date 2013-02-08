package com.osinka.subset

import com.mongodb.{BasicDBObjectBuilder,DBObject}

object DBO {
  def empty: DBObjectBuffer = new DBObjectBuffer(BasicDBObjectBuilder.start)

  def apply[A : BsonWritable](m: Map[String,A]): DBObjectBuffer = apply(m.toList:_*)

  def apply[A : BsonWritable](tuples: (String,A)*): DBObjectBuffer = {
    val buffer = empty
    for ((k,a) <- tuples)
      buffer.append(k, a)
    buffer
  }

  implicit val asBson = BsonWritable[DBObjectBuffer](_())
}

class DBObjectBuffer(builder: BasicDBObjectBuilder) {
  def append[A](k: String, value: A)(implicit writer: BsonWritable[A]): this.type = {
    writer(value) foreach {v => builder.add(k, v)}
    this
  }

  def append[A : BsonWritable](t: (String,A)): this.type = append(t._1, t._2)

  // case class Binding(name: String, value: Any)
  // bindings: Binding*
  //def on[A : BsonWritable](bindings: (Symbol,A)*): DBObject

  def apply(): DBObject = builder.get
}
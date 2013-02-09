package com.osinka.subset

import org.bson.types.{Symbol => BsonSymbol}
import com.mongodb.{BasicDBObjectBuilder,DBObject}

object DBO {
  def empty: DBObjectBuffer =
    new DBObjectBuffer(BasicDBObjectBuilder.start)

  def apply(tuples: KV*): DBObjectBuffer = {
    val buffer = BasicDBObjectBuilder.start
    for {KV(key,option) <- tuples
         value <- option}
      buffer.add(key, value)
    new DBObjectBuffer(buffer)
  }

  case class KV(key: String, value: Option[Any])
}

class DBObjectBuffer(val builder: BasicDBObjectBuilder) {
  def append[A](k: String, value: A)(implicit writer: BsonWritable[A]): this.type = {
    writer(value) match {
      case Some(v) => builder.add(k, v)
      case None =>
    }
    this
  }

  def append[A : BsonWritable](t: (String,A)): this.type = append(t._1, t._2)

  /* walks through the DBObject and replaces all the value Symbols by corresponding bindings.
   * The intended use is like this
   * ```
   * DBO("a" -> 'a).on('a -> "value")
   * ```
   */
  def on(bindings: DBO.KV*): DBObject = {
    val m = Map(bindings map { binding => binding.key -> binding.value } :_*)

    def walkDBObject(dbo: DBObject): DBObject = {
      import collection.JavaConverters._

      for {k <- dbo.keySet.asScala}
        dbo.get(k) match {
          case inner: DBObject =>
            walkDBObject(inner)
          case sym: BsonSymbol if m.contains(sym.getSymbol) =>
            m(sym.getSymbol) match {
              case Some(value) => dbo.put(k, value)
              case None => dbo.removeField(k)
            }
          case _ =>
        }

      dbo
    }

    walkDBObject(this.apply())
  }

  def apply(): DBObject = builder.get
}

object DBObjectBuffer {
  implicit val asBson = BsonWritable[DBObjectBuffer](_())
}
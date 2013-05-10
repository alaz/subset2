package com.osinka.subset

import org.bson.types.{Symbol => BsonSymbol}
import com.mongodb.{BasicDBObjectBuilder,BasicDBObject,DBObject}

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

/**
 * Mutable buffer. Produces [[com.mongodb.DBObject]]
 */
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
   *
   * ```
   * DBO("a" -> 'a) ('a -> "value")
   * ```
   *
   * Nested subdocuments will be searched for replacement as well:
   *
   * ```
   * DBO("a" -> DBO("b" -> 'val)) ('val -> 123)
   * ```
   *
   * Binding values are not looked up for replacement
   */
  def apply(bindings: DBO.KV*): DBObject = {
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

    val dbo = buildSideEffectFreeObject
    if (bindings.isEmpty)
      dbo
    else
      walkDBObject(dbo)
  }

  /** For those who got used to Anorm
    */
  def on(bindings: DBO.KV*): DBObject = apply(bindings:_*)

  /*
   * In fact, BasicDBObjectBuilder is mutable through the DBObject it creates.
   *
   * When you call `get` on BasicDBObjectBuilder and then modify the resulting
   * DBObject, the structures inside the builder mutate as well. So the next object
   * created will be not what your builder looks like. I know, this is weird.
   *
   * The helper method below creates a "deep copy" of DBObject so that you
   * could modify the builder freely.
   */
  private def buildSideEffectFreeObject =
    builder.get.asInstanceOf[BasicDBObject].copy.asInstanceOf[DBObject]

}

object DBObjectBuffer {
  implicit val asBson = BsonWritable[DBObjectBuffer](_())
}
package com.osinka

package object subset {
  import scala.language.implicitConversions

  implicit def kvFromStrKey[A](t: (String,A))(implicit writer: BsonWritable[A]) =
    DBO.KV(t._1, writer(t._2))

  implicit def kvFromSymKey[A](t: (Symbol,A))(implicit writer: BsonWritable[A]) =
    DBO.KV(t._1.name, writer(t._2))
}
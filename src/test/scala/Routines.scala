package com.osinka.subset

trait Routines {
  def unpack[T](o: Any)(implicit f: Field[T]) = f(o)
  def pack[T](x: T)(implicit writer: BsonWritable[T]): Option[Any] = writer(x)
}

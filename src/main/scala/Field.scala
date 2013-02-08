package com.osinka.subset

import annotation.implicitNotFound
import java.util.Date
import java.util.regex.Pattern
import util.matching.Regex
import org.bson.types.{ObjectId, Binary, Symbol => BsonSymbol}
import com.mongodb.DBObject

@implicitNotFound(msg = "Cannot find Field for ${A}")
trait Field[+A] {
  def apply(o: Any): Option[A]
}

case class FieldPf[+T](pf: PartialFunction[Any, T]) extends Field[T] {
  override def apply(o: Any): Option[T] = PartialFunction.condOpt(o)(pf)

  def orElse[B1 >: T](pf2: PartialFunction[Any,B1]): FieldPf[B1] = copy(pf = pf orElse pf2)
  def orElse[B1 >: T](g: FieldPf[B1]): FieldPf[B1] = orElse(g.pf)

  def andThen[R](pf2: PartialFunction[T,R]) =
    copy(pf = new PartialFunction[Any,R] {
        override def isDefinedAt(x: Any) = pf.isDefinedAt(x) && pf2.isDefinedAt(pf(x))
        override def apply(x: Any): R = pf2(pf(x))
      })

}

object Field {
  import collection.JavaConverters._
  import org.bson.types.BasicBSONList

  def apply[T](pf: PartialFunction[Any,T]): FieldPf[T] = new FieldPf[T](pf)

  //
  // Default readers
  //

  implicit val booleanGetter = Field[Boolean]({ case b: Boolean => b })
  implicit val intGetter = Field[Int]({ case i: Int => i })
  implicit val shortGetter = Field[Short]({ case i: Short => i })
  implicit val longGetter = Field[Long]({ case l: Long => l })
  implicit val floatGetter = Field[Float]({
      case d: Double => d.floatValue
      case f: Float => f
    })
  implicit val doubleGetter = Field[Double]({ case d: Double => d })
  implicit val dateGetter = Field[Date]({ case d: Date => d })

  implicit val objIdGetter = Field[ObjectId]({ case objId: ObjectId => objId })
  implicit val dboGetter = Field[DBObject]({ case dbo: DBObject => dbo })
  implicit val patternGetter = Field[Pattern]({ case p: Pattern => p })
  implicit val stringGetter = Field[String]({
    case s: String => s
    case s: BsonSymbol => s.getSymbol
    case oid: ObjectId => oid.toStringMongod
  })
  implicit val symbolGetter = Field[Symbol]({
    case s: Symbol => s
    case s: BsonSymbol => Symbol(s.getSymbol)
  })
  implicit val regexGetter = Field[Regex]({
    case p: Pattern => new Regex(p.pattern)
    case r: Regex => r
  })

  implicit def byteArrayGetter = Field[Array[Byte]]({
      case b: Binary => b.getData
      case a: Array[Byte] => a
    })
  implicit def arrayGetter[T](implicit r: Field[T], m: Manifest[T]) =
    Field[Array[T]]({
      case a: Array[_] => a.asInstanceOf[Array[T]]
      case list: BasicBSONList => list.asScala.flatMap(r.apply _).toArray
    })

  implicit def optionGetter[T](implicit r: Field[T]) =
    new Field[Option[T]] {
      override def apply(o: Any): Option[Option[T]] = Some(r.apply(o))
    }
  implicit def listGetter[T](implicit r: Field[T]) =
    Field[List[T]]({
      case ar: Array[_] => ar.flatMap(r.apply _).toList
      case list: BasicBSONList => list.asScala.flatMap(r.apply _).toList
    })
  implicit def tuple2Getter[T1,T2](implicit r1: Field[T1], r2: Field[T2]) =
    new Field[Tuple2[T1,T2]] {
      override def apply(o: Any): Option[Tuple2[T1,T2]] =
        o match {
          case a: Array[_] if a.size == 2 =>
            for {v1 <- r1.apply(a(0)); v2 <- r2.apply(a(1))}
            yield (v1, v2)
        }
    }
  // TODO: Field[Map[String,T]]
}
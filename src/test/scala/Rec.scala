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

    lazy val Doc: DocParser[Rec] =
      get[Int]("id") ~ get[List[Rec]]("children").opt map {
        case id ~ children => new Rec(id, children)
      }

    override def apply(o: Any): Option[Rec] =
      PartialFunction.condOpt(o) {
        case Doc(rec) => rec
      }
  }
  def Doc = recField.Doc // so that we don't need to change the spec
}

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

  // reader
  import DocParser._

  implicit lazy val recField = Field({ case Doc(rec) => rec })
  lazy val Doc: DocParser[Rec] = get[Int]("id") ~ get[List[Rec]]("children").opt map { case id ~ children => new Rec(id, children) }
}

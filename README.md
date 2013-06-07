# Getting Started

**Subset** 2.x provides simple and extensible APIs:

* to build `DBObject` structures for subsequent use in MongoDB driver API

  in type-safe, Anorm-like manner
* to parse the resulting `DBObject` documents

  in terms of parser combinators


## DBObject builder

MongoDB Java driver commonly accepts `DBObject` values as arguments to
various query methods.  Thus we need a simple way to create `DBObject`
documents assuming we have different field types.

This is where a mutable `DBObjectBuffer` object comes in handy

```scala
import com.osinka.subset._

val buffer = DBO("email" -> "user@domain.tld", "name" -> "John Doe")
buffer.append("age" -> 30)
```

In order to create a `DBObject`, just call `apply` method on `buffer`:

```scala
collection.save(buffer())
```

### Own serializers

Every value supplied into `DBO` gets serialzed by `BsonWritable[T]`
type class. Hence you may easily create own serialzers for your types,
e.g. if you have a type

```scala
case class Likes(count: Int, latest: java.util.Date)
```

you may write the corresponding `BsonWritable`:

```scala
object Likes {
  implicit val asBson = BsonWritable[Likes](likes =>
    DBO("count" -> likes.count, "latest" -> likes.latest)()
  )
}
```

and then

```scala
val dbo = DBO("likes" -> Likes(5, new java.util.Date())) ()
```

**Subset** already contains an extensive library of serializers for
Scala/Java primitive types

### Parameters in builder

Scala symbols get transformed into BSON symbols. But `DBObjectBuilder`
lets you replace any symbol value later:

```scala
val preparedStmt = DBO("user.age" -> DBO("$gt" -> 'age))
collection.find(preparedStmt('age -> 12))
```

Actually you may drop some values as well by supplying `None`:

```scala
val preparedStmt = DBO("post.version" -> 'version, "modt" -> DBO("$gt" -> 'datetime))
preparedStmt('version -> (None:Option[Int]), 'datetime -> new java.util.Date)
```

Expectedly, `Some` will just work as plain value too:

```scala
val preparedStmt = DBO("user.age" -> DBO("$gt" -> 'age))
collection.find(preparedStmt('age -> Some(12)))
```

## Parser API

If you liked
[Parser API in Play2's Anorm](http://www.playframework.com/documentation/2.0/ScalaAnorm),
you'll quickly get the idea of composable document parser combinators
in **Subset**

You may get a typed field from a document by the field's name:

```scala
import DocParser._
val parseCount: DocParser[Int] = get[Int]("count")
```

A parser is merely a function `DBObject => Either[String,A]`, thus you
would apply it as follows:

```scala
parseCount(collection.findOne(query)) fold (msg => ..., count => ...)
```

Any parser provides `unapply` method as well for use in pattern
matching (in case you don't need parsing failure)

```scala
val counts =
  collection.find(query).asScala collect {
    case parseCount(count) => count
  }
```

Parsers are composable. `int("count") ~ get[java.util.Date]("latest")`
will create a `DocParser[Int ~ Date]`, thus it parses tuples of `Int`
and `Date`. It's possible to transform these tuples into `Like` types
then:

```scala
val likes = int("count") ~ get[java.util.Date]("latest") map {
    case count ~ latest => new Likes(count,latest)
  }
```

Just like any parser combinator library, **Subset** provides *option*
and *alternative*. You may transform any `DocParser[A]` into
`DocParser[Option[A]]`, e.g.

```scala
val maybeLikes: DocParser[Option[Like]] = likes.opt
```

And method `|` lets you select between parsers:

```scala
val logEntry: DocParser[LogEntry] = {
  val ver1: DocParser[LogEntry] = int("f") map {i => LogEntryV1(i)}
  val ver2: DocParser[LogEntry] = str("s") map {s => LogEntryV2(s)}
  
  (contains("version", 1) ~> ver1 |
   contains("version", 2) ~> ver2)
}
```

**Subset** has a number of parsers specific to MongoDB documents. It
lets you parse `ObjectId` values with `oid(name)` parser. `docId` is
simply `oid("_id")` and fits for document IDs. Since MongoDB documents
are hierarchical, there is a parser to dig deeper into the
subdocuments, it's called `doc[A](name: String)(p: DocParser[A])`. If
you know you have a subdocument `user` holding `User` you would write
something like

```scala
val logEntryWithUser: DocParser[LogEntry] = logEntry ~ doc("user")(userParser)
```

Since MongoDB provides a *dot-syntax* to dig into documents,
**Subset** does the same:

```scala
val userName: DocParser[String] = get[String]("user" :: "name" :: Nil)
```

As a matter of personal preference I would write it as
`get[String]("user.name" split "\\.")`

#### `get[Option[T]](fieldName)` vs. `get[T](fieldName).opt`

When you create a parser `get[Option[T]](fieldName)` you declare there
must be a field named `fieldName`, but you are not sure if it can be
decoded. Which means, such parser will fail if there is not field. It
will return `Some[T]` if it could decode the value and `None`
otherwise.

But when you create `get[T](fieldName).opt` you declare the field is
optional. The parser will return `None` if no field with this name
exists and `Some[T]` if the field exists. Certainly it will fail if it
cannot decode the field.

### Smarter deserialization

Any primitive `get` parser relies on type class `Field[A]` that can
retrieve values of type `A` from `Any` (the field value from
`DBObject`). **Subset** already contains a library of such
deserializers in two flavours. The default library gets included when
you do `import com.osinka.subset._` and it is quite strict, e.g. it
cannot decode `ObjectId` from a `String`. However, if you do `import
SmartFields._` before your parsers, they will do their best to decode
*compatible* types. E.g. they will accept `Int` value when asked to
parse `Long`, etc.

### Own fields

You are free to define own `Field[A]` implicits:

```scala
implicit val jodaDateTime = Field[DateTime]({
    case date: Date => new DateTime(date)
  })
}
```

# Installation

[Instructions](http://ls.implicit.ly/osinka/subset2#subset)

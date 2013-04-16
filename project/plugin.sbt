addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.4.0")

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com"
)

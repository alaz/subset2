import ls.Plugin._

organization := "com.osinka.subset"

name := "subset"

homepage := Some(url("https://github.com/osinka/subset2"))

startYear := Some(2013)

scalaVersion := "2.10.0"

licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")

organizationName := "Osinka"

description := """MongoDB Document parser combinators and builders"""

scalacOptions ++= List("-deprecation", "-unchecked", "-feature")

libraryDependencies ++= Seq(
  "org.mongodb" % "mongo-java-driver" % "2.10.1",
  "org.scalatest" % "scalatest" % "1.8" % "test" cross CrossVersion.full
)

credentials += Credentials(Path.userHome / ".ivy2" / "credentials_sonatype")

pomIncludeRepository := { x => false }

publishTo <<= (version) { version: String =>
  if (version.trim endsWith "SNAPSHOT")
    Some(Resolver.file("file", file(Path.userHome.absolutePath+"/.m2/repository")))
  else
    Some("Sonatype OSS Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/")
}

seq(lsSettings: _*)

(LsKeys.tags in LsKeys.lsync) := Seq("mongo", "mongodb")

//(LsKeys.docsUrl in LsKeys.lsync) := Some(url("http://osinka.github.com/subset2/Subset.html"))

pomExtra := <xml:group>
    <developers>
      <developer>
        <id>alaz</id>
        <email>azarov@osinka.com</email>
        <name>Alexander Azarov</name>
        <timezone>+4</timezone>
      </developer>
    </developers>
    <scm>
      <connection>scm:git:git://github.com/osinka/subset2.git</connection>
      <developerConnection>scm:git:git@github.com:osinka/subset2.git</developerConnection>
      <url>http://github.com/osinka/subset2</url>
    </scm>
    <issueManagement>
      <system>github</system>
      <url>http://github.com/osinka/subset2/issues</url>
    </issueManagement>
  </xml:group>

name := "br.com.diego.processor.nats-message-processor"

version := "0.1"

scalaVersion := "2.13.4"

resolvers += Resolver.mavenLocal

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val akkaV = "2.6.10"
  val akkaHttpV = "10.2.1"
  val circeVersion = "0.13.0"
  Seq(
    akka %% "akka-actor-typed" % akkaV,
    akka %% "akka-stream-typed" % akkaV,
    akka %% "akka-cluster-tools" % akkaV,
    akka %% "akka-cluster-sharding-typed" % akkaV,
    akka %% "akka-serialization-jackson" % akkaV,
    akka %% "akka-persistence-typed" % akkaV,
    akka %% "akka-persistence-query" % akkaV,
    akka %% "akka-persistence-cassandra" % "1.0.4",

    akka %% "akka-slf4j" % akkaV,
    akka %% "akka-http" % akkaHttpV,

    "ch.qos.logback" % "logback-classic" % "1.2.3",

    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,

    "de.heikoseeberger" %% "akka-http-circe" % "1.35.2",

    "ch.megard" %% "akka-http-cors" % "1.1.0",
    "io.nats" % "java-nats-streaming" % "2.2.3",

    "org.codehaus.groovy" % "groovy-all" % "3.0.7" pomOnly()
  )
}


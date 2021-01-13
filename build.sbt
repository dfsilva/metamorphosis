name := "metamorphosis"

version := "0.0.1"

scalaVersion := "2.13.4"

resolvers += Resolver.mavenLocal

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val akkaV = "2.6.10"
  val akkaHttpV = "10.2.1"
  val circeVersion = "0.13.0"
  val slickVersion = "3.3.2"
  Seq(
    akka %% "akka-actor-typed" % akkaV,
    akka %% "akka-stream-typed" % akkaV,
    akka %% "akka-cluster-tools" % akkaV,
    akka %% "akka-cluster-sharding-typed" % akkaV,
    akka %% "akka-serialization-jackson" % akkaV,
    akka %% "akka-persistence-typed" % akkaV,
    akka %% "akka-persistence-query" % akkaV,
    "com.lightbend.akka" %% "akka-persistence-jdbc" % "4.0.0",
    "com.typesafe.slick" %% "slick" % slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
    "com.github.tminglei" %% "slick-pg" % "0.19.4",
    "com.github.tminglei" %% "slick-pg_circe-json" % "0.19.4",

    "org.flywaydb" % "flyway-core" % "7.2.0",
    "org.postgresql" % "postgresql" % "42.2.18",

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


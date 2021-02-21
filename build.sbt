import sbtassembly.MergeStrategy

name := "metamorphosis"
organization := "br.com.diegosilva"
version := "0.0.1"
scalaVersion := "2.13.4"

enablePlugins(DockerPlugin)

resolvers += Resolver.mavenLocal

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val akkaV = "2.6.12"
  val akkaHttpV = "10.2.3"
  val circeVersion = "0.13.0"
  val slickVersion = "3.3.3"
  Seq(
    akka %% "akka-actor-typed" % akkaV,
    akka %% "akka-stream-typed" % akkaV,
    akka %% "akka-cluster-tools" % akkaV,
    akka %% "akka-cluster-sharding-typed" % akkaV,
    akka %% "akka-serialization-jackson" % akkaV,
    akka %% "akka-persistence-typed" % akkaV,
    akka %% "akka-persistence-query" % akkaV,
    "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.0",
    "com.typesafe.slick" %% "slick" % slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
    "com.github.tminglei" %% "slick-pg" % "0.19.4",
    "com.github.tminglei" %% "slick-pg_circe-json" % "0.19.4",

    "org.flywaydb" % "flyway-core" % "7.5.2",
    "org.postgresql" % "postgresql" % "42.2.18",

    akka %% "akka-slf4j" % akkaV,
    akka %% "akka-http" % akkaHttpV,

    "ch.qos.logback" % "logback-classic" % "1.2.3",

    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,

    "de.heikoseeberger" %% "akka-http-circe" % "1.35.3",

    "io.nats" % "java-nats-streaming" % "2.2.3",

    "org.codehaus.groovy" % "groovy-all" % "3.0.7" pomOnly()
  )
}

assemblyJarName in assembly := "server.jar"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.discard
    }
  case PathList("reference.conf") => MergeStrategy.concat
  case _ => MergeStrategy.first
}


dockerfile in docker := {
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("openjdk:11-jre")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

buildOptions in docker := BuildOptions(cache = false)


import NativePackagerHelper._

name := "bitcoin-price-api"

version := "0.1"

scalaVersion := "2.12.4"
enablePlugins(JavaServerAppPackaging)

val json = "3.5.3"
val akkaHttpJson4s = "1.18.1"
val akkaHttpSession = "0.5.3"
val akkaHttpSwagger = "0.12.0"
val commonValidator = "1.5.1"
val mockitoCoreVersion = "1.9.5"
val akkaHttp = "10.0.10"
val swaggerUi = "3.17.1"
val scalaLogging = "3.5.0"
val ficus = "1.4.3"
val config = "1.3.2"
val logbackClassic = "1.1.7"
val scalatest = "3.0.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttp,
  "com.github.swagger-akka-http" %% "swagger-akka-http" % akkaHttpSwagger,
  "org.webjars" % "swagger-ui" % swaggerUi,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttp % "test",
  "org.json4s" %% "json4s-ext" % json,
  "de.heikoseeberger" %% "akka-http-json4s" % akkaHttpJson4s,
  "com.softwaremill.akka-http-session" %% "core" % akkaHttpSession,
  "com.softwaremill.akka-http-session" %% "jwt" % akkaHttpSession,
  "commons-validator" % "commons-validator" % commonValidator,
  "org.mockito" % "mockito-core" % mockitoCoreVersion,
  "com.iheart" %% "ficus" % ficus,
  "org.scalatest" %% "scalatest" % scalatest % "test"
).map(_.exclude("org.slf4j", "*")) ++
  Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLogging,
    "ch.qos.logback" % "logback-classic" % logbackClassic
  )


mappings in Universal ++= {
  val configs = ((baseDirectory in Compile).value / "src/main/resources").listFiles()
  configs.map {f =>
    f -> s"conf/${f.getName}"
  }.toSeq
}
scriptClasspath := "../conf/" +: scriptClasspath.value
mainClass in Compile := Some("com.bitcoin.price.app.PriceApp")

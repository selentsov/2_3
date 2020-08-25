scalaVersion := "2.13.3"

val akkaVersion = "2.6.8"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
)

libraryDependencies += "com.typesafe" % "config" % "1.4.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

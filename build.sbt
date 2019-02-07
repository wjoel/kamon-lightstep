scalaVersion := "2.12.6"

resolvers += Resolver.bintrayRepo("kamon-io", "snapshots")

libraryDependencies += "com.lightstep.tracer" % "lightstep-tracer-jre" % "0.14.8"
libraryDependencies += "com.lightstep.tracer" % "tracer-grpc" % "0.15.10"
libraryDependencies += "io.grpc" % "grpc-netty" % "1.14.0"
libraryDependencies += "io.netty" % "netty-tcnative-boringssl-static" % "2.0.12.Final"
libraryDependencies += "io.kamon" %% "kamon-core" % "1.1.3"

lazy val root = (project in file("."))
  .settings(
    organization := "com.wjoel",
    name := "kamon-lightstep",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.11.12",
    libraryDependencies ++= Seq(
      "com.lightstep.tracer" % "lightstep-tracer-jre" % "0.14.8",
      "com.lightstep.tracer" % "tracer-grpc" % "0.15.10",
      "io.grpc" % "grpc-netty" % "1.14.0",
      "io.netty" % "netty-tcnative-boringssl-static" % "2.0.12.Final",
      "io.kamon" %% "kamon-core" % "1.1.3"
    )
  )

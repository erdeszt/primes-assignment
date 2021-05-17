
lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.13.5",
)

lazy val protocol = (project in file("protocol"))
  .settings(commonSettings)
  .settings(
    name := "protocol",
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true) -> (sourceManaged in Compile).value / "scalapb",
      scalapb.zio_grpc.ZioCodeGenerator -> (sourceManaged in Compile).value / "scalapb",
    ),
    // TODO: Cleanup deps
    libraryDependencies ++= Seq(
      "io.grpc" % "grpc-netty" % "1.37.1",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    ),
  )

lazy val proxyServer = (project in file("proxy-server"))
  .dependsOn(protocol)
  .settings(commonSettings)
  .settings(
    name := "proxy-server",
    // TODO: Cleanup deps
    libraryDependencies ++= Seq(
      "io.grpc" % "grpc-netty" % "1.37.1",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    ),
  )

lazy val primeServer = (project in file("prime-server"))
  .dependsOn(protocol)
  .settings(commonSettings)
  .settings(
    name := "prime-server",
    // TODO: Cleanup deps
    libraryDependencies ++= Seq(
      "io.grpc" % "grpc-netty" % "1.37.1",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    ),
  )

lazy val root = (project in file("."))
  .aggregate(protocol, proxyServer, primeServer)

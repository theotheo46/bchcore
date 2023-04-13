import sbt.Keys.{libraryDependencies, resolvers, sourceManaged}
import sbt.{Resolver, _}
import sbtprotoc.ProtocPlugin.autoImport.PB


/**
 * @author Alexey Polubelov
 */
object Dependencies {
    val ProjectScalaVersion = "2.12.10"
    //    val SPRING_BOOT_VERSION = "2.3.1.RELEASE"
    val BC_VERSION = "1.60"
    val SCALA_PB_VERSION = "0.11.0"
    val GRPC_VERSION = "1.9.0"
    val MONOCLE_VERSION = "2.1.0"

    val JettyVersion = "11.0.1"
    val HttpClientVersion = "4.5.13"
    val UpickleVersion = "1.3.8"
    val ScalaJSDomVersion = "1.1.0"

    val DefaultResolvers = Seq(
        Resolver.mavenLocal,
        Resolver.DefaultMavenRepository,
        "jitpack" at "https://jitpack.io" //for com.github.everit-org.json-schema:org.everit.json.schema (dep from fabric-chaincode-shim)
    )

    val Testing = Seq(
        // testing libraries
        "org.scalatest" %% "scalatest" % "3.2.5" % Test,
        "org.scalatest" %% "scalatest-funsuite" % "3.2.5" % Test
    )

    val MockingJVM = Seq(
        "org.mockito" % "mockito-core" % "3.8.0" % Test
    )

    val LoggingJVM = Seq(
        "org.slf4j" % "slf4j-api" % "1.7.30",
        "ch.qos.logback" % "logback-classic" % "1.2.3" // depends on logback-core
    )

    val Postgres = Seq(
        "io.getquill" % "quill-core_2.12" % "3.7.0",
        "org.postgresql" % "postgresql" % "42.2.8",
        "io.getquill" %% "quill-jdbc" % "3.7.0",
        "com.zaxxer" % "HikariCP" % "3.4.5")

    val GRPCCore = Seq(
        "io.grpc" % "grpc-protobuf" % GRPC_VERSION,
        "io.grpc" % "grpc-stub" % GRPC_VERSION,
        "io.grpc" % "grpc-netty" % GRPC_VERSION,
        "io.grpc" % "grpc-netty-shaded" % GRPC_VERSION,
        "io.netty" % "netty-tcnative-boringssl-static" % "2.0.7.Final"
        //TODO: use binding to openSSL:    "io.netty" % "netty-tcnative" % "2.0.7.Final" classifier "linux-x86_64"
    )

    val BouncyCastle = Seq(
        "org.bouncycastle" % "bcprov-jdk15on" % BC_VERSION,
        "org.bouncycastle" % "bcpkix-jdk15on" % BC_VERSION
    )

    val xml = Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.6")

    //lazy val PostgresDriver = "org.postgresql" % "postgresql" % "42.2.14"

    //    val Spring = Seq(
    //        "org.springframework.boot" % "spring-boot-starter-web" % SPRING_BOOT_VERSION,
    //        "org.springdoc" % "springdoc-openapi-ui" % "1.2.32",
    //        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.11.1"
    //    )

    lazy val ScalaPBSetting = Seq(
        Compile / PB.targets := Seq(
            //      PB.gens.java(PROTOBUF_VERSION) -> (Compile / sourceManaged).value,
            scalapb.gen() -> (Compile / sourceManaged).value
        ),
        libraryDependencies -= "com.thesamet.scalapb" %% "scalapb-runtime" % SCALA_PB_VERSION,
        libraryDependencies += ("com.thesamet.scalapb" %% "scalapb-runtime" % SCALA_PB_VERSION)
            .exclude("io.grpc", "*")
            .exclude("io.netty", "*")
            .exclude("com.google.protobuf", "protobuf-java")
            .exclude("com.google.protobuf", "protobuf-java-util"),
        libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
        libraryDependencies ++= GRPCCore
    )

    lazy val JAXB_FOR_JAVA_11 = Seq(
        "com.sun.xml.bind" % "jaxb-core" % "2.3.0.1",
        "javax.xml.bind" % "jaxb-api" % "2.3.1",
        "com.sun.xml.bind" % "jaxb-impl" % "2.3.1"
    )
}

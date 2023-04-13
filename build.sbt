import Dependencies._
import sbt.Keys.{artifactPath, libraryDependencies}

import java.nio.file.{Files, StandardCopyOption}

ThisBuild / scalaVersion := "2.12.10"
ThisBuild / organization := "ru.sberbank.blockchainlab.cnft"
ThisBuild / version := "4.1.0-SNAPSHOT"
// TODO: set jvm version to java 11
//ThisBuild / javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
//ThisBuild / scalacOptions += "-target:jvm-1.8"
ThisBuild / resolvers ++= DefaultResolvers
ThisBuild / scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8", // yes, this is 2 args
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    //    "-Ywarn-dead-code", // N.B. doesn't work well with the ??? hole
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Ywarn-unused-import",
    "-Xfatal-warnings"
)

// ============================== VERSIONS ================================

val junitVersion = "4.13.2"
val cucumberVersion = "6.10.4"

// ========================================================================

lazy val root = project.in(file("."))
    .settings(
        name := "sber-token-core"
    )
    .aggregate(
        protocol,
        chaincode,

        gate,

        wallet_lib_js,
        wallet_lib_jvm,

        wallet_lib_model_js,
        wallet_lib_model_jvm,

        wallet_generator,

        tools,

        integration_test,
        //frontend,
        issuer_example,
        wallet_remote,

        timestamp_data_feed,
        megacuks_service,

        migration_tests_model,
        migration_tests
    )

// ========================================================================
// The protocol
// ========================================================================
lazy val protocol = project.in(file("protocol"))
    .aggregate(
        model_jvm,
        model_js,

        spec_jvm,
        spec_js,

        engine
    )

// ========================================================================
lazy val model = crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Full).in(file("protocol/model"))
    .settings(
        name := "model",
        ScalaPBSetting,
        Compile / PB.protoSources := Seq(file("protocol/model/shared/model"))
    )
    .jvmSettings(
        libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided"
        //libraryDependencies ++= LoggingJVM,
    )
    .dependsOn(types)

lazy val model_jvm = model.jvm.settings(name := "model_jvm")
lazy val model_js = model.js.settings(name := "model_js")

// ========================================================================
lazy val spec = crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Full).in(file("protocol/spec"))
    .settings(
        name := "spec",
        libraryDependencies ++= Seq(
            "com.lihaoyi" %%% "upickle" % UpickleVersion
        )
    )
    .jvmSettings(
        libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided"
    )
    .dependsOn(model, cryptography)

lazy val spec_jvm = spec.jvm.settings(name := "spec_jvm")
lazy val spec_js = spec.js.settings(name := "spec_js")

// ========================================================================

lazy val engine = project.in(file("protocol/engine"))
    .settings(
        name := "engine",
        libraryDependencies ++= Seq(
            "com.google.guava" % "guava" % "19.0"
        ),
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM
    )
    .dependsOn(spec_jvm, commons_jvm, scalapb_codec_jvm)

// ========================================================================
// The chaincode
// ========================================================================
lazy val chaincode = project.in(file("chaincode"))
    .aggregate(
        chaincode_impl,
        chaincode_spec
    )

lazy val chaincode_spec = project.in(file("chaincode/chaincode-spec"))
    .settings(
        name := "chaincode-spec"
    )
    .dependsOn(spec_jvm, contract_spec)

lazy val chaincode_impl = project.in(file("chaincode/chaincode-impl"))
    .settings(
        name := "chaincode-impl",
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM
    )
    .dependsOn(chaincode_spec, engine, scalapb_codec_jvm, codecs, fabric_chaincode_scala)
    .enablePlugins(JavaAppPackaging)

// ========================================================================
// The Gate
// ========================================================================
lazy val gate = project.in(file("gate"))
    .aggregate(
        gate_impl,
        gate_spec_jvm,
        gate_spec_js
    )

lazy val gate_impl = project.in(file("gate/gate-impl"))
    .settings(
        name := "gate-impl",
        //        libraryDependencies ++= FabricChainCodeClient,
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM
    )
    .dependsOn(gate_spec_jvm, chaincode_spec, http_service_server, scalapb_codec_jvm, codecs, fabric_chaincode_client)
    .enablePlugins(JavaAppPackaging)

lazy val gate_spec = crossProject(JVMPlatform, JSPlatform).in(file("gate/gate-spec"))
    .settings(
        name := "gate-spec"
    )
    .jvmSettings(
        libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided"
    )
    .dependsOn(http_service_meta, spec)

lazy val gate_spec_jvm = gate_spec.jvm.settings(name := "gate_spec_jvm")
lazy val gate_spec_js = gate_spec.js.settings(name := "gate_spec_js")

// ========================================================================
// Wallet library
// ========================================================================


//lazy val wallet_lib = project.in(file("wallet-lib"))
//  .aggregate(
//      wallet_lib_jvm,
//      wallet_lib_js,
//
//      wallet_lib_model_js,
//      wallet_lib_model_jvm
//  )

val makeNPM = taskKey[Unit]("Creates NPM package")
val walletLib = "wallet-lib"
val npmCompileOpt = fastOptJS
val moduleType = ModuleKind.CommonJSModule

lazy val lib = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Full).in(file("wallet-lib"))
    .settings(
        name := walletLib,
        ScalaPBSetting,
    )
    .jvmSettings(
        libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0", // % "provided" // - need to bundle it in JVM for remote wallet to wark
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM,
        libraryDependencies ++= Postgres
    )
    .jsSettings(
        libraryDependencies ++= Seq(
            "com.lihaoyi" %%% "upickle" % UpickleVersion,
            "io.github.cquiroz" %%% "scala-java-time" % "2.2.2"
        ),
        // export module:
        scalaJSLinkerConfig ~= {
            _.withModuleKind(moduleType)
        },
        makeNPM := {
            // generate library:
            (Compile / npmCompileOpt).value

            //
            val outDir = new File((Compile / target).value.getCanonicalPath + "/npm")
            outDir.mkdirs()

            // copy files:
            val libFile = (Compile / npmCompileOpt / artifactPath).value
            val libJSFile = new File(outDir.getCanonicalPath + s"/main.js")
            Files.copy(libFile.toPath, libJSFile.toPath, StandardCopyOption.REPLACE_EXISTING)

            //
            val packageJSONFile = new File(outDir.getCanonicalPath + "/package.json")
            IO.write(packageJSONFile,
                s"""|{
                    |   "main": "./main.js",
                    |   "name": "wallet-lib",
                    |   "version": "0.0.1"
                    |}
                    |""".stripMargin
            )

            println(s"NPM package created at: $outDir")

            println("Generating TS interfaces ...")
            val tsPath = outDir.getCanonicalPath + "/index.d.ts"
            (Compile / runner).value.run(
                mainClass = "PrintTS",
                classpath = (Compile / fullClasspath).value.files,
                options = Seq(tsPath),
                log = streams.value.log
            )
            println(s"TS interfaces generated at: $tsPath")
        }
    )
    .dependsOn(gate_spec, http_service_client, cryptography, wallet_lib_model, scalapb_codec)

lazy val wallet_lib_jvm = lib.jvm.settings(name := "wallet_lib_jvm")
lazy val wallet_lib_js = lib.js.settings(name := "wallet_lib_js")

lazy val wallet_lib_model = crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Full).in(file("wallet-lib-model"))
    .settings(
        name := "wallet-lib-model",
        ScalaPBSetting,
        Compile / PB.protoSources := Seq(file("wallet-lib-model/shared/model"))
    )
    .jvmSettings(
        libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided"
        //libraryDependencies ++= LoggingJVM,
    )
    .dependsOn(model)


lazy val wallet_lib_model_jvm = wallet_lib_model.jvm.settings(name := "wallet_lib_model_jvm")
lazy val wallet_lib_model_js = wallet_lib_model.js.settings(name := "wallet_lib_model_js")

// ========================================================================
// Tool for generating wallet information
// ========================================================================
lazy val wallet_generator = project.in(file("wallet-generator"))
    .settings(
        name := "wallet-generator"
    )
    .dependsOn(wallet_lib_jvm)
    .enablePlugins(JavaAppPackaging)


// ========================================================================
// Remote Wallet
// ========================================================================
lazy val wallet_remote = project.in(file("wallet-remote"))
    .settings(
        name := "wallet-remote",
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM
    )
    .dependsOn(http_service_server, scalapb_codec_jvm, codecs, wallet_lib_jvm)
    .enablePlugins(JavaAppPackaging)

// ========================================================================
// Timestamp Data Feed
// ========================================================================
lazy val timestamp_data_feed = project.in(file("timestamp-data-feed"))
    .settings(
        name := "timestamp-data-feed",
        libraryDependencies ++= LoggingJVM
    )
    .dependsOn(wallet_lib_jvm)
    .enablePlugins(JavaAppPackaging)

// ========================================================================
// Megacuks service
// ========================================================================
lazy val megacuks_service = project.in(file("megacuksservice"))
    .settings(
        name := "megacuksservice",
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM
    )
    .dependsOn(http_service_server, scalapb_codec_jvm, codecs, wallet_lib_jvm)
    .enablePlugins(JavaAppPackaging)


// ========================================================================
// Tools
// ========================================================================
lazy val tools = project.in(file("tools"))
    .aggregate(
        commons_jvm,
        commons_js,

        types_jvm,
        types_js,

        utility_jvm,
        utility_js,

        http_service_meta_jvm,
        http_service_meta_js,

        http_service_client_jvm,
        http_service_client_js,

        http_service_server,

        cryptography_jvm,
        cryptography_js,

        cryptography_model_jvm,
        cryptography_model_js,

        scalapb_codec_jvm,
        scalapb_codec_js,

        contract_spec,

        codecs,
        fabric_chaincode_client,
        fabric_chaincode_scala

    )


// =====================================================================================================================
lazy val commons = crossProject(JVMPlatform, JSPlatform).in(file("tools/commons"))
    .settings(
        name := "commons"
    )
    .jvmSettings(
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM
    )
    .jsSettings(
        libraryDependencies ++= Seq(
            "org.scala-js" %%% "scalajs-dom" % ScalaJSDomVersion
        )
    )

lazy val commons_jvm = commons.jvm.settings(name := "commons_jvm")
lazy val commons_js = commons.js.settings(name := "commons_js")

// =====================================================================================================================
lazy val types = crossProject(JVMPlatform, JSPlatform).in(file("tools/types"))
    .settings(
        name := "types"
    )
    .jvmSettings(
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM
    )
    .jsSettings(
        libraryDependencies ++= Seq(
            "org.scala-js" %%% "scalajs-dom" % ScalaJSDomVersion
        )
    )
    .dependsOn(commons)

lazy val types_jvm = types.jvm.settings(name := "types_jvm")
lazy val types_js = types.js.settings(name := "types_js")

// =====================================================================================================================
lazy val utility = crossProject(JVMPlatform, JSPlatform).in(file("tools/utility"))
    .settings(
        name := "utility",
        libraryDependencies ++= Seq(
            "org.scala-lang" % "scala-reflect" % ProjectScalaVersion,
            "com.lihaoyi" %%% "upickle" % UpickleVersion % Test
        ),
        // TODO: use correct dependencies for JS (no idea how this work for JS, but it is)
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM
    )
    .dependsOn(types)

lazy val utility_jvm = utility.jvm.settings(name := "utility_jvm")
lazy val utility_js = utility.js.settings(name := "utility_js")

// =====================================================================================================================
lazy val http_service_meta = crossProject(JVMPlatform, JSPlatform).in(file("tools/http-service-meta"))
    .settings(
        name := "http-service-meta"
    )

lazy val http_service_meta_jvm = http_service_meta.jvm.settings(name := "http_service_meta_jvm")
lazy val http_service_meta_js = http_service_meta.js.settings(name := "http_service_meta_js")

// =====================================================================================================================
lazy val http_service_server = project.in(file("tools/http-service-server"))
    .settings(
        name := "http-service-server",
        libraryDependencies ++= Seq(
            ("org.eclipse.jetty" % "jetty-server" % JettyVersion).exclude("org.slf4j", "slf4j-api"),
            ("org.eclipse.jetty" % "jetty-servlet" % JettyVersion).exclude("org.slf4j", "slf4j-api"),
            ("org.eclipse.jetty" % "jetty-servlets" % JettyVersion).exclude("org.slf4j", "slf4j-api"),
            "com.lihaoyi" %% "upickle" % UpickleVersion
        ),
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM
    )
    .dependsOn(utility_jvm, http_service_meta_jvm)

// =====================================================================================================================
lazy val http_service_client = crossProject(JVMPlatform, JSPlatform)
    .in(file("tools/http-service-client"))
    .settings(
        name := "http-service-client",
        libraryDependencies ++= Seq(
            "com.lihaoyi" %%% "upickle" % UpickleVersion
        ),
        // TODO: use correct dependencies for JS (no idea how this work for JS, but it is)
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM
    )
    .jvmSettings(
        libraryDependencies ++= Seq(
            ("org.apache.httpcomponents" % "httpclient" % HttpClientVersion)
                .exclude("org.slf4j", "slf4j-api")
                .exclude("org.apache.logging.log4j", "log4j-api")
                .exclude("org.apache.logging.log4j", "log4j-core")
            ,
            "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided"
        )
    )
    .jsSettings(
        libraryDependencies ++= Seq(
            "org.scala-js" %%% "scalajs-dom" % ScalaJSDomVersion
        )
    )
    .dependsOn(utility, http_service_meta)

lazy val http_service_client_jvm = http_service_client.jvm.settings(name := "http_service_client_jvm")
lazy val http_service_client_js = http_service_client.js.settings(name := "http_service_client_js")

// =====================================================================================================================
lazy val cryptography_model = crossProject(JVMPlatform, JSPlatform).in(file("tools/cryptography-model"))
    .settings(
        name := "cryptography-model",
        ScalaPBSetting,
        Compile / PB.protoSources := Seq(file("tools/cryptography-model/shared/model"))
    )
    .jvmSettings(
        libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided"
    )
    .dependsOn(types)

lazy val cryptography_model_jvm = cryptography_model.jvm.settings(name := "cryptography_model_jvm")
lazy val cryptography_model_js = cryptography_model.js.settings(name := "cryptography_model_js")

// =====================================================================================================================
lazy val cryptography = crossProject(JVMPlatform, JSPlatform).in(file("tools/cryptography"))
    .settings(
        name := "cryptography"
    )
    .jvmSettings(
        libraryDependencies ++= BouncyCastle,
        libraryDependencies ++= xml,
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM,
        libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided",
        libraryDependencies ++= Postgres
    )
    .jsSettings(
        libraryDependencies ++= Seq(
            "com.lihaoyi" %%% "upickle" % UpickleVersion
        )
    )
    .dependsOn(cryptography_model, utility, http_service_meta)


lazy val cryptography_jvm = cryptography.jvm.settings(name := "cryptography_jvm").dependsOn(http_service_client_jvm)
lazy val cryptography_js = cryptography.js
    .settings(
        name := "cryptography_js",
        jsDependencies += ProvidedJS / "gostextutils.js",
        packageJSDependencies / skip := false
    )
    .dependsOn(http_service_client_js, utility_js)
    .enablePlugins(JSDependenciesPlugin)


// =====================================================================================================================
lazy val scalapb_codec = crossProject(JVMPlatform, JSPlatform).in(file("tools/scalapb-codec"))
    .settings(
        name := "scalapb-codec",
        libraryDependencies += ("com.thesamet.scalapb" %% "scalapb-runtime" % SCALA_PB_VERSION)
            .exclude("io.grpc", "*")
            .exclude("io.netty", "*")
            //.exclude("com.google.protobuf", "protobuf-java")
            .exclude("com.google.protobuf", "protobuf-java-util")
    )
    .jvmSettings(
        libraryDependencies ++= LoggingJVM,
        libraryDependencies ++= Testing,
        libraryDependencies ++= MockingJVM
    )
    .jsSettings(
        libraryDependencies ++= Seq(
            "org.scala-js" %%% "scalajs-dom" % ScalaJSDomVersion
        )
    )
    .dependsOn(utility)

lazy val scalapb_codec_jvm = scalapb_codec.jvm.settings(name := "scalapb_codec_jvm")
    .dependsOn(contract_spec)
lazy val scalapb_codec_js = scalapb_codec.js.settings(name := "scalapb_codec_js")

lazy val contract_spec = project.in(file("tools/contract-spec"))
    .settings(
        name := "contract-spec"
    )


lazy val codecs = project.in(file("tools/codecs"))
    .settings(
        name := "codecs",
        libraryDependencies += "com.google.code.gson" % "gson" % "2.7",

        libraryDependencies += "junit" % "junit" % "4.12" % Test,
        libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.0.5" % Test
    )
    .dependsOn(
        scalapb_codec_jvm,
        contract_spec
    )

lazy val fabric_chaincode_client = project.in(file("tools/fabric-chaincode-client"))
    .settings(
        name := "fabric-chaincode-client",
        libraryDependencies ++= Seq(
            ("org.hyperledger.fabric-sdk-java" % "fabric-sdk-java" % "2.2.0")
                .exclude("commons-logging", "commons-logging")
                .exclude("io.grpc", "*")
                .exclude("io.netty", "*")
                .exclude("com.google.protobuf", "protobuf-java")
                .exclude("com.google.protobuf", "protobuf-java-util")
                .exclude("org.miracl.milagro.amcl", "milagro-crypto-java")
                .exclude("org.bouncycastle", "bcprov-jdk15on")
                .exclude("org.bouncycastle", "bcpkix-jdk15on")
                .exclude("org.apache.logging.log4j", "log4j-api")
                .exclude("org.apache.logging.log4j", "log4j-core")
                .exclude("org.apache.logging.log4j", "log4j-1.2-api"))
            ++ GRPCCore ++ BouncyCastle ++ JAXB_FOR_JAVA_11,
        libraryDependencies ++= LoggingJVM,
        libraryDependencies += "org.slf4j" % "jcl-over-slf4j" % "1.7.30",
        libraryDependencies += "org.slf4j" % "jul-to-slf4j" % "1.7.30"
    )
    .dependsOn(contract_spec, utility_jvm)


lazy val fabric_chaincode_scala = project.in(file("tools/fabric-chaincode-scala"))
    .settings(
        name := "fabric-chaincode-scala",
        libraryDependencies += "org.hyperledger.fabric-chaincode-java" % "fabric-chaincode-shim" % "2.2.2",
        libraryDependencies += "junit" % "junit" % "4.12" % Test,
        libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.0.5" % Test,
        libraryDependencies += "org.mockito" % "mockito-core" % "2.23.0" % Test
    )
    .dependsOn(codecs, contract_spec, utility_jvm)

// ========================================================================
// Integration-test
// ========================================================================
lazy val integration_test = project.in(file("integration-test"))
    .settings(
        name := "integration-test",
        libraryDependencies ++=
            Seq(
                "junit" % "junit" % junitVersion % Test,
                "io.cucumber" % "cucumber-junit" % cucumberVersion % Test,
                "io.zonky.test" % "embedded-postgres" % "1.3.1" % Test,
                "org.flywaydb" % "flyway-core" % "7.9.0" % Test,
                "io.cucumber" %% "cucumber-scala" % cucumberVersion % Test,
                postgres("io.zonky.test.postgres", "11.12.0") % Test
            ),
        CucumberPlugin.glues := List("ru.sberbank.blockchain.cnft.wallet.test")
    )
    .dependsOn(wallet_lib_jvm, wallet_lib_model_jvm)
    .enablePlugins(CucumberPlugin)

// ========================================================================
// Issuer_example
// ========================================================================
lazy val issuer_example = project.in(file("issuer-example"))
    .settings(
        name := "issuer-example"
    )
    .dependsOn(wallet_lib_jvm)
    .enablePlugins(JavaAppPackaging)

// ========================================================================
// MigrationTests
// ========================================================================
lazy val migration_tests = project.in(file("migration-tests"))
    .settings(
        name := "migration-tests"
    )
    .dependsOn(wallet_lib_jvm, migration_tests_model)
    .enablePlugins(JavaAppPackaging)


lazy val migration_tests_model = project.in(file("migration-tests/migration-tests-model"))
    .settings(
        name := "migration-tests-model",
        ScalaPBSetting,
    )
    .dependsOn(wallet_lib_model_jvm)

// ========================================================================
// Utils
// ========================================================================
def postgres(artifact: String, version: String): ModuleID = {
    val osVersion = System.getProperty("os.name").toLowerCase match {
        case osName if osName.contains("mac") =>
            "embedded-postgres-binaries-darwin-amd64"
        case osName if osName.contains("win") =>
            "embedded-postgres-binaries-windows-amd64"
        case osName if osName.contains("linux") =>
            "embedded-postgres-binaries-linux-amd64"
        case osName => throw new RuntimeException(s"Unknown operating system $osName")
    }
    artifact % osVersion % version
}

//// ========================================================================
//// The frontend
//// ========================================================================
//val FrontendBundlePath = file("frontend/bundle/main")
//
//lazy val frontend = project.in(file("frontend"))
//    .settings(
//        name := "frontend",
//        scalaJSUseMainModuleInitializer := true,
//        mainClass := Some("ru.sberbank.blockchain.frontend.WalletFrontend"),
//        libraryDependencies ++= Seq(
//            "org.scala-js" %%% "scalajs-dom" % ScalaJSDomVersion,
//            "com.github.japgolly.scalajs-react" %%% "core" % "1.7.7",
//            "io.github.cquiroz" %%% "scala-java-time" % "2.2.0",
//            "com.lihaoyi" %%% "upickle" % UpickleVersion,
//            "com.github.julien-truffaut" %%% "monocle-core" % MONOCLE_VERSION,
//            "com.github.julien-truffaut" %%% "monocle-macro" % MONOCLE_VERSION,
//            "com.github.julien-truffaut" %%% "monocle-law" % MONOCLE_VERSION % Test
//        ),
//        jsDependencies ++= Seq(
//
//            "org.webjars.npm" % "react" % "16.7.0"
//                / "umd/react.development.js"
//                minified "umd/react.production.min.js"
//                commonJSName "React",
//
//            "org.webjars.npm" % "react-dom" % "16.7.0"
//                / "umd/react-dom.development.js"
//                minified "umd/react-dom.production.min.js"
//                dependsOn "umd/react.development.js"
//                commonJSName "ReactDOM",
//
//            "org.webjars.npm" % "react-dom" % "16.7.0"
//                / "umd/react-dom-server.browser.development.js"
//                minified "umd/react-dom-server.browser.production.min.js"
//                dependsOn "umd/react-dom.development.js"
//                commonJSName "ReactDOMServer"
//        ),
//        // Target files for Scala.js plugin
//        Compile / fastOptJS / artifactPath := FrontendBundlePath / "frontend.js",
//        Compile / fullOptJS / artifactPath := FrontendBundlePath / "frontend.js",
//        Compile / packageJSDependencies / artifactPath := FrontendBundlePath / "frontend-deps.js",
//        Compile / packageMinifiedJSDependencies / artifactPath := FrontendBundlePath / "frontend-deps.js",
//
//        // to enable macro annotations:
//        addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.full),
//    )
//    .dependsOn(wallet_lib_js)
//    .enablePlugins(ScalaJSPlugin)
//    .enablePlugins(JSDependenciesPlugin)
//

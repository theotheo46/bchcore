addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0")
//addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.6.0")
addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.6")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("com.waioeka.sbt" % "cucumber-plugin" % "0.3.1")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.0"

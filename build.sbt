name := "live-app-versions"

organization := "com.gu"

description:= "Lambda function which retrieves the latest beta version from App Store Connect."

version := "1.0"

scalaVersion := "2.12.8"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-lambda-java-log4j2" % "1.1.0",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.8.2",
  "com.pauldijou" %% "jwt-core" % "4.2.0",
  "com.squareup.okhttp3" % "okhttp" % "4.3.1",
  "com.turo" % "pushy" % "0.13.9",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.slf4j" % "slf4j-api" % "1.7.30"
)

enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")

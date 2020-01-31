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
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.714",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.1", // Pin a more recent version to avoid Snyk vulnerabilities introduced by s3 sdk
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-lambda-java-log4j2" % "1.1.0",
  "com.gu" %% "simple-configuration-ssm" % "1.4.1",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.8.2",
  "com.pauldijou" %% "jwt-core" % "4.2.0",
  "com.squareup.okhttp3" % "okhttp" % "4.3.1",
  "com.turo" % "pushy" % "0.13.9",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)

enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
assemblyMergeStrategy in assembly := {
  case "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat" => MergeStrategy.last
  case "META-INF/io.netty.versions.properties" => MergeStrategy.discard
  case "module-info.class" => MergeStrategy.discard //See: https://stackoverflow.com/a/55557287
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")

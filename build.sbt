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
  "com.amazonaws" % "aws-java-sdk-s3" % "1.12.101",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-lambda-java-log4j2" % "1.5.0",
  "com.google.auth" % "google-auth-library-oauth2-http" % "0.20.0",
  "com.gu" %% "simple-configuration-ssm" % "1.5.7",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.0",
  "com.pauldijou" %% "jwt-core" % "4.2.0",
  "com.squareup.okhttp3" % "okhttp" % "4.3.1",
  "com.eatthepath" % "pushy" % "0.15.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "junit" % "junit" % "4.13.1"
)

enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
assembly / assemblyMergeStrategy := {
  case "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat" => MergeStrategy.last
  case "META-INF/io.netty.versions.properties" => MergeStrategy.discard
  case "module-info.class" => MergeStrategy.discard //See: https://stackoverflow.com/a/55557287
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case x => MergeStrategy.first
}

riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")

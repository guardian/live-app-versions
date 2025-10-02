name := "live-app-versions"

organization := "com.gu"

description:= "Lambda function which retrieves the latest beta version from App Store Connect."

version := "1.0"

scalaVersion := "2.13.16"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-release:21",
  "-Ywarn-dead-code"
)

val circeVersion = "0.14.15"
val jjwtVersion = "0.13.0"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % "1.12.783",
  "com.amazonaws" % "aws-lambda-java-core" % "1.3.0",
  "com.amazonaws" % "aws-lambda-java-log4j2" % "1.5.1",
  "com.gu" %% "simple-configuration-ssm" % "5.1.2",
  "com.google.auth" % "google-auth-library-oauth2-http" % "1.39.1",
  "org.slf4j" % "slf4j-simple" % "2.0.17",
  "com.github.jwt-scala" %% "jwt-core" % "11.0.3",
  "com.squareup.okhttp3" % "okhttp" % "5.1.0",
  "com.eatthepath" % "pushy" % "0.15.4",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.slf4j" % "slf4j-api" % "2.0.17",
  "org.scalatest" %% "scalatest" % "3.2.19" % "test",
  "junit" % "junit" % "4.13.2",
  "io.netty" % "netty-codec-http2" % "4.2.6.Final",
  "io.netty" % "netty-handler-proxy" % "4.2.0.Final",
  "io.netty" % "netty-resolver-dns" % "4.2.0.Final",
  "io.netty" % "netty-transport-native-epoll" % "4.2.0.Final",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.18.4.1",
  "org.kohsuke" % "github-api" % "1.330",
  "io.jsonwebtoken" % "jjwt-impl" % jjwtVersion,
  "io.jsonwebtoken" % "jjwt-jackson" % jjwtVersion % Runtime,
  "org.bouncycastle" % "bcpkix-jdk15on" % "1.70",
)

assemblyJarName := s"${name.value}.jar"
assembly / assemblyMergeStrategy := {
  case "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat" => MergeStrategy.last
  case "META-INF/io.netty.versions.properties" => MergeStrategy.discard
  case "module-info.class" => MergeStrategy.discard //See: https://stackoverflow.com/a/55557287
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case x => MergeStrategy.first
}

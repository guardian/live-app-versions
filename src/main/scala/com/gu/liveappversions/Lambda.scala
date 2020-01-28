package com.gu.liveappversions

import com.amazonaws.services.lambda.runtime.Context
import com.gu.liveappversions.Config.{ AppStoreConnectConfig, Env }
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Success }

object Lambda {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def handler(context: Context): Unit = {
    val env = Env()
    logger.info(s"Starting $env")
    process(env, "static-content-dist")
  }

  def process(env: Env, uploadBucketName: String): Unit = {
    val appStoreConnectConfig = AppStoreConnectConfig(env)
    val token = JwtTokenBuilder.buildToken(appStoreConnectConfig)
    val generateBuildOutputAttempt = for {
      appStoreConnectResponse <- AppStoreConnectApi.getLatestBetaBuilds(token, appStoreConnectConfig)
      buildOutput <- BuildOutput.fromAppStoreConnectResponse(appStoreConnectResponse)
    } yield buildOutput
    generateBuildOutputAttempt match {
      case Success(buildOutput) =>
        logger.info(s"The latest iOS beta with external beta testers is: ${buildOutput.latestReleasedBuild}. Previous versions are: ${buildOutput.previouslyReleasedBuilds}")
        S3Uploader.attemptUpload(buildOutput, env, uploadBucketName)
      case Failure(ex) => logger.error(s"Failed to retrieve the latest build information from App Store Connect due to: $ex")
    }

  }

}

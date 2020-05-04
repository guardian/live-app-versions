package com.gu.liveappversions

import com.amazonaws.services.lambda.runtime.Context
import com.gu.appstoreconnectapi.{ AppStoreConnectApi, JwtTokenBuilder }
import com.gu.config.Config.{ AppStoreConnectConfig, Env }
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
    val attempt = for {
      latestBetas <- AppStoreConnectApi.getLatestBetaBuilds(token, appStoreConnectConfig)
      buildOutput <- BuildOutput.findLatestBuildsWithExternalTesters(latestBetas)
      uploadAttempt <- S3Uploader.attemptUpload(buildOutput, env, uploadBucketName)
    } yield uploadAttempt
    attempt match {
      case Success(_) =>
        logger.info("Successfully updated build information")
      case Failure(exception) =>
        logger.error(s"Failed to update build information due to $exception")
        throw exception // This allows us to monitor failures easily (using standard AWS metrics)
    }
  }

}

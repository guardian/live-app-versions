package com.gu.liveappversions

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.model.PutObjectResult
import com.gu.liveappversions.Config.{ AppStoreConnectConfig, Env }
import org.slf4j.{ Logger, LoggerFactory }

object Lambda {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def handler(context: Context): Unit = {
    val env = Env()
    logger.info(s"Starting $env")
    process(env, "static-content-dist")
  }

  def process(env: Env, uploadBucketName: String): PutObjectResult = {
    val appStoreConnectConfig = AppStoreConnectConfig(env)
    val token = JwtTokenBuilder.buildToken(appStoreConnectConfig)
    val attempt = for {
      appStoreConnectResponse <- AppStoreConnectApi.getLatestBetaBuilds(token, appStoreConnectConfig)
      buildOutput <- BuildOutput.fromAppStoreConnectResponse(appStoreConnectResponse)
      uploadAttempt <- S3Uploader.attemptUpload(buildOutput, env, uploadBucketName)
    } yield uploadAttempt
    attempt.get // This will throw an exception if something failed (which allows us to monitor failures easily)
  }

}

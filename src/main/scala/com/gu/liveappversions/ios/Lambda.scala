package com.gu.liveappversions.ios

import com.amazonaws.services.lambda.runtime.Context
import com.gu.appstoreconnectapi.{ AppStoreConnectApi, JwtTokenBuilder }
import com.gu.config.Config.{ AppStoreConnectConfig, Env }
import com.gu.liveappversions.{ S3Storage, UploadAttempt }
import io.circe.syntax._
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
      uploadAttempt <- S3Storage.attemptUpload(buildOutput.asJson, env, uploadBucketName, "ios-live-app/recent-beta-releases.json")
    } yield uploadAttempt
    UploadAttempt.handle(attempt)
  }

}

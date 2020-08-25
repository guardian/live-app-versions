package com.gu.liveappversions.android

import com.gu.config.Config.Env
import com.gu.liveappversions.{ S3Storage, UploadAttempt }
import com.gu.playdeveloperapi.PlayDeveloperApi.PlayDeveloperApi
import com.gu.playdeveloperapi.Token
import io.circe.syntax._
import org.slf4j.{ Logger, LoggerFactory }

object Lambda {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def handler(): Unit = {
    val env = Env()
    logger.info(s"Starting $env")
    process(env, "static-content-dist")
  }

  def process(env: Env, uploadBucketName: String) = {
    val attempt = for {
      token <- Token.getToken(env)
      versions <- PlayDeveloperApi.getBetaAndProductionVersions(token)
      uploadAttempt <- S3Storage.attemptUpload(BuildInfo(true).asJson, env, uploadBucketName, s"android-live-app/versions/${versions.currentBeta.versionName}.json")
    } yield {
      uploadAttempt
    }
    UploadAttempt.handle(attempt)
  }

}

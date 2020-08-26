package com.gu.liveappversions.android

import com.gu.config.Config.Env
import com.gu.liveappversions.android.VersionWithTracks.VersionJson
import com.gu.liveappversions.{ S3Storage, UploadAttempt }
import com.gu.playdeveloperapi.Conversion.{ AndroidLiveAppVersions, Version }
import com.gu.playdeveloperapi.PlayDeveloperApi.PlayDeveloperApi
import com.gu.playdeveloperapi.Token
import io.circe.parser
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Success, Try }

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
      googlePlayVersions <- PlayDeveloperApi.getBetaAndProductionVersions(token)
      s3Versions <- getVersionsFromS3(env, uploadBucketName, googlePlayVersions)
      updatesRequired = Updates.getRequiredUpdates(googlePlayVersions, s3Versions)
      uploadUpdates <- Updates.performUpdates(updatesRequired, env, uploadBucketName)
    } yield {
      uploadUpdates
    }
    UploadAttempt.handle(attempt)
  }

  def partialKey(version: Version): String = s"android-live-app/versions/${version.name}.json"

  def getVersionsFromS3(env: Env, uploadBucketName: String, versions: AndroidLiveAppVersions): Try[List[VersionWithTracks]] = {

    def readBuildInfo(version: Version): Try[VersionWithTracks] = {
      S3Storage.getJsonString(env, uploadBucketName, partialKey(version)) match {
        case Success(Some(jsonString)) =>
          parser.decode[VersionJson](jsonString).map(versionJson => VersionJson.toVersionWithTracks(versionJson)).toTry
        case Success(None) =>
          Success(VersionWithTracks(version, Set.empty))
        case Failure(exception) =>
          Failure(exception)
      }
    }

    val versionsToCheck = List(versions.currentBeta, versions.currentProduction).distinct

    val readAttempts = versionsToCheck.map {
      version => readBuildInfo(version)
    }

    Try(readAttempts.map(_.get))

  }

}

package com.gu.liveappversions.android

import com.amazonaws.services.s3.model.PutObjectResult
import com.gu.config.Config.Env
import com.gu.liveappversions.S3Storage
import com.gu.liveappversions.android.Lambda.{ logger, partialKey }
import com.gu.liveappversions.android.VersionWithTracks.VersionJson
import com.gu.playdeveloperapi.Conversion.AndroidLiveAppVersions
import io.circe.syntax._

import scala.util.Try

object Updates {

  def getRequiredUpdates(googlePlayVersions: AndroidLiveAppVersions, s3Versions: List[VersionWithTracks]): List[Option[VersionWithTracks]] = {
    s3Versions.map { versionWithTracks =>
      val currentGooglePlayTracks = VersionWithTracks.toTracks(
        beta = googlePlayVersions.currentBeta == versionWithTracks.version,
        production = googlePlayVersions.currentProduction == versionWithTracks.version)
      val version = versionWithTracks.version
      val s3TrackHistory = versionWithTracks.tracks
      val fullTrackHistory: Set[Track] = s3TrackHistory ++ currentGooglePlayTracks // Duplicates will be removed as we're using Set
      if (fullTrackHistory.equals(s3TrackHistory)) {
        logger.info(s"File in s3 for version $version is up to date, no update required")
        None
      } else {
        logger.info(s"File in s3 for version $version requires updates. Previous track info $s3TrackHistory | New track info: $fullTrackHistory")
        Some(VersionWithTracks(version, fullTrackHistory))
      }
    }
  }

  def performUpdates(updates: List[Option[VersionWithTracks]], env: Env, uploadBucketName: String): Try[List[PutObjectResult]] = {

    val toUpdate = updates.filter(_.isDefined)

    val performUpdates = toUpdate.map { update =>
      logger.info(s"Performing update for Android version ${update.get.version}")
      S3Storage.putJson(VersionJson.fromVersionWithTracks(update.get).asJson, env, uploadBucketName, partialKey(update.get.version))
    }

    Try(performUpdates.map(_.get))

  }

}

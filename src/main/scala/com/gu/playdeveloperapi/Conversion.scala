package com.gu.playdeveloperapi

import com.gu.playdeveloperapi.PlayDeveloperApi.PlayDeveloperApi.{ Track, TracksResponse }

import scala.util.{ Failure, Success, Try }

object Conversion {

  case class Build(versionName: String)
  case class AndroidLiveAppVersions(currentBeta: Build, currentProduction: Build)

  case object PlayDeveloperApiConversionException extends Throwable

  def searchForTrack(allTracks: List[Track], trackName: String): Option[Build] = {
    allTracks
      .find(_.track == trackName)
      .map(track => track.releases)
      .flatMap(_.find(_.status == "completed"))
      .map(build => Build(build.name))
  }

  def toAndroidLiveAppVersions(tracksResponse: TracksResponse): Try[AndroidLiveAppVersions] = {

    val allTracks = tracksResponse.tracks

    val searchForBeta: Option[Build] = searchForTrack(allTracks, "beta")
    val searchForProduction: Option[Build] = searchForTrack(allTracks, "production")

    (searchForBeta, searchForProduction) match {
      case (Some(beta), Some(production)) => Success(
        AndroidLiveAppVersions(currentBeta = beta, currentProduction = production))
      case _ => Failure(PlayDeveloperApiConversionException)
    }

  }

}

package com.gu.playdeveloperapi

import com.gu.playdeveloperapi.PlayDeveloperApi.PlayDeveloperApi.{ Track, TracksResponse }

import scala.util.{ Failure, Success, Try }

object Conversion {

  case class Version(name: String)
  case class AndroidLiveAppVersions(currentBeta: Version, currentProduction: Version)

  case object PlayDeveloperApiConversionException extends Throwable

  def searchForTrack(allTracks: List[Track], trackName: String): Option[Version] = {
    allTracks
      .find(_.track == trackName)
      .map(track => track.releases)
      .flatMap(_.find(_.status == "completed"))
      .map(build => Version(build.name))
  }

  def toAndroidLiveAppVersions(tracksResponse: TracksResponse): Try[AndroidLiveAppVersions] = {

    val allTracks = tracksResponse.tracks

    val searchForBeta: Option[Version] = searchForTrack(allTracks, "beta")
    val searchForProduction: Option[Version] = searchForTrack(allTracks, "production")

    (searchForBeta, searchForProduction) match {
      case (Some(beta), Some(production)) => Success(
        AndroidLiveAppVersions(currentBeta = beta, currentProduction = production))
      case _ => Failure(PlayDeveloperApiConversionException)
    }

  }

}

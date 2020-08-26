package com.gu.liveappversions.android

import com.gu.playdeveloperapi.Conversion.Version
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto._

sealed trait Track
case object Beta extends Track
case object Production extends Track

case class VersionWithTracks(version: Version, tracks: Set[Track])

object VersionWithTracks {

  def toTracks(beta: Boolean, production: Boolean): Set[Track] = (beta, production) match {
    case (false, false) => Set.empty
    case (true, false) => Set(Beta)
    case (false, true) => Set(Production)
    case (true, true) => Set(Beta, Production)
  }

  implicit val versionJsonDecoder: Decoder[VersionJson] = deriveDecoder
  implicit val versionWithTracksEncoder: Encoder[VersionJson] = deriveEncoder

  case class VersionJson(versionName: String, seenInBeta: Boolean, seenInProduction: Boolean)

  object VersionJson {
    def toVersionWithTracks(versionJson: VersionJson): VersionWithTracks = {
      VersionWithTracks(Version(versionJson.versionName), toTracks(versionJson.seenInBeta, versionJson.seenInProduction))
    }
    def fromVersionWithTracks(versionWithTracks: VersionWithTracks): VersionJson = {
      val tracks = versionWithTracks.tracks
      VersionJson(
        versionName = versionWithTracks.version.name,
        seenInBeta = tracks.contains(Beta),
        seenInProduction = tracks.contains(Production))
    }
  }

}

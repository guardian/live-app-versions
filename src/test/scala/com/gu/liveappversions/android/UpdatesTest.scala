package com.gu.liveappversions.android

import com.gu.playdeveloperapi.Conversion.{ AndroidLiveAppVersions, Version }
import org.scalatest.funsuite.AnyFunSuite

class UpdatesTest extends AnyFunSuite {

  val newVersion = Version("1.2.4")
  val existingVersion = Version("1.2.3")
  val oldProductionVersion = Version("1.2.2")

  val s3BrandNewVersion = VersionWithTracks(newVersion, Set.empty)
  val s3ExistingBetaVersion = VersionWithTracks(existingVersion, Set(Beta))
  val s3FullyPromotedVersion = VersionWithTracks(oldProductionVersion, Set(Beta, Production))

  test("getRequiredUpdates should correctly determine when a new beta has been released") {

    val s3Versions = List(s3BrandNewVersion, s3FullyPromotedVersion)
    val playVersions = AndroidLiveAppVersions(currentBeta = newVersion, currentProduction = oldProductionVersion)

    val result = Updates.getRequiredUpdates(playVersions, s3Versions)
    val expectedChange = Some(VersionWithTracks(newVersion, Set(Beta)))

    assert(result.contains(expectedChange))

  }

  test("getRequiredUpdates should correctly determine when a build has gone straight to production") {

    val s3Versions = List(s3ExistingBetaVersion, s3BrandNewVersion)
    val playVersions = AndroidLiveAppVersions(currentBeta = existingVersion, currentProduction = newVersion)

    val result = Updates.getRequiredUpdates(playVersions, s3Versions)
    val expectedChange = Some(VersionWithTracks(newVersion, Set(Production)))

    assert(result.contains(expectedChange))

  }

  test("getRequiredUpdates should correctly determine when a build has been promoted to production") {

    val s3Versions = List(s3ExistingBetaVersion)
    val playVersions = AndroidLiveAppVersions(currentBeta = existingVersion, currentProduction = existingVersion)

    val result = Updates.getRequiredUpdates(playVersions, s3Versions)
    val expectedChange = Some(VersionWithTracks(existingVersion, Set(Beta, Production)))

    assert(result.contains(expectedChange))

  }

  test("getRequiredUpdates should not suggest an update for a version which has already been seen in both tracks") {

    val s3Versions = List(s3FullyPromotedVersion)
    val playVersions = AndroidLiveAppVersions(currentBeta = oldProductionVersion, currentProduction = oldProductionVersion)

    val result = Updates.getRequiredUpdates(playVersions, s3Versions)
    val expected = List(None)

    assert(result === expected)

  }

  test("getRequiredUpdates should not suggest an update for a version which has already been seen in beta") {

    val s3Versions = List(s3ExistingBetaVersion, s3FullyPromotedVersion)
    val playVersions = AndroidLiveAppVersions(currentBeta = existingVersion, currentProduction = oldProductionVersion)

    val result = Updates.getRequiredUpdates(playVersions, s3Versions)
    val expected = List(None, None)

    assert(result === expected)

  }

}

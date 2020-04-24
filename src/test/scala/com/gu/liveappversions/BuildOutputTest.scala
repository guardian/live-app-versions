package com.gu.liveappversions

import java.time.ZonedDateTime

import com.gu.appstoreconnectapi.AppStoreConnectApi.{BetaBuildAttributes, BetaBuildDetails, BuildAttributes, BuildDetails, BuildsResponse}
import org.scalatest.FunSuite

class BuildOutputTest extends FunSuite {

  val now = ZonedDateTime.now()

  val releasedToExternalBetaDetails = BetaBuildDetails("id123", BetaBuildAttributes("IN_BETA_TESTING"))
  val releasedToExternalBuildDetails = BuildDetails("id123", BuildAttributes("8.16 (5)", now))

  val unreleasedToExternalBetaDetails = BetaBuildDetails("id999", BetaBuildAttributes("IN_REVIEW"))
  val unreleasedToExternalBuildDetails = BuildDetails("id999", BuildAttributes("8.16 (56)", now))

  test("findLatestBuildsWithExternalTesters should correctly identify the 4 most recent beta versions from Apple's response") {
    val buildDetails = List(
      releasedToExternalBuildDetails,
      releasedToExternalBuildDetails.copy(id = "id124").copy(attributes = BuildAttributes("8.16 (4)", now)),
      releasedToExternalBuildDetails.copy(id = "id125").copy(attributes = BuildAttributes("8.16 (3)", now)),
      releasedToExternalBuildDetails.copy(id = "id126").copy(attributes = BuildAttributes("8.16 (2)", now)),
      releasedToExternalBuildDetails.copy(id = "id127").copy(attributes = BuildAttributes("8.16 (1)", now))
    )
    val betaDetails = List(
      releasedToExternalBetaDetails,
      releasedToExternalBetaDetails.copy(id = "id124"),
      releasedToExternalBetaDetails.copy(id = "id125"),
      releasedToExternalBetaDetails.copy(id = "id126"),
      releasedToExternalBetaDetails.copy(id = "id127")
    )
    val buildsResponse = BuildsResponse(buildDetails, betaDetails)
    val result = BuildOutput.findLatestBuildsWithExternalTesters(buildsResponse)

    assert(result.get.latestReleasedBuild.version === "8.16 (5)")
    assert(result.get.previouslyReleasedBuilds.map(_.version) === List("8.16 (4)", "8.16 (3)", "8.16 (2)"))

  }

  test("findLatestBuildsWithExternalTesters should ignore betas which have NOT been released to external testers") {
    val buildDetails = List(
      unreleasedToExternalBuildDetails,
      releasedToExternalBuildDetails,
      releasedToExternalBuildDetails.copy(id = "id124").copy(attributes = BuildAttributes("8.16 (4)", now)),
      releasedToExternalBuildDetails.copy(id = "id125").copy(attributes = BuildAttributes("8.16 (3)", now)),
      releasedToExternalBuildDetails.copy(id = "id126").copy(attributes = BuildAttributes("8.16 (2)", now)),
    )
    val betaDetails = List(
      unreleasedToExternalBetaDetails,
      releasedToExternalBetaDetails,
      releasedToExternalBetaDetails.copy(id = "id124"),
      releasedToExternalBetaDetails.copy(id = "id125"),
      releasedToExternalBetaDetails.copy(id = "id126")
    )
    val buildsResponse = BuildsResponse(buildDetails, betaDetails)
    val result = BuildOutput.findLatestBuildsWithExternalTesters(buildsResponse)

    assert(result.get.latestReleasedBuild.version === "8.16 (5)")
    assert(result.get.previouslyReleasedBuilds.map(_.version) === List("8.16 (4)", "8.16 (3)", "8.16 (2)"))

  }

  test("findLatestBuildsWithExternalTesters should fail if there are less than 4 recent beta versions") {
    val buildDetails = List(releasedToExternalBuildDetails, releasedToExternalBuildDetails, releasedToExternalBuildDetails)
    val betaDetails = List(releasedToExternalBetaDetails, releasedToExternalBetaDetails, releasedToExternalBetaDetails)
    val buildsResponse = BuildsResponse(buildDetails, betaDetails)
    val result = BuildOutput.findLatestBuildsWithExternalTesters(buildsResponse)
    assert(result.isFailure)
  }

  test("findLatestBuildsWithExternalTesters should fail if it cannot find the version name for the most recent beta") {
    val buildDetails = List(
      releasedToExternalBuildDetails.copy("fakeId"),
      releasedToExternalBuildDetails.copy(id = "id124").copy(attributes = BuildAttributes("8.16 (4)", now)),
      releasedToExternalBuildDetails.copy(id = "id125").copy(attributes = BuildAttributes("8.16 (3)", now)),
      releasedToExternalBuildDetails.copy(id = "id126").copy(attributes = BuildAttributes("8.16 (2)", now)),
    )
    val betaDetails = List(
      releasedToExternalBetaDetails,
      releasedToExternalBetaDetails.copy(id = "id124"),
      releasedToExternalBetaDetails.copy(id = "id125"),
      releasedToExternalBetaDetails.copy(id = "id126")
    )
    val buildsResponse = BuildsResponse(buildDetails, betaDetails)
    val result = BuildOutput.findLatestBuildsWithExternalTesters(buildsResponse)
    assert(result.isFailure)
  }

  test("findPreviousThreeBetaVersions should fail if it finds less than 3 previous versions (due to mismatched ids)") {
    val allBetas = List(
      releasedToExternalBetaDetails,
      releasedToExternalBetaDetails.copy(id = "id124"),
      releasedToExternalBetaDetails.copy(id = "id125"),
      releasedToExternalBetaDetails.copy(id = "id126")
    )
    val buildDetails = List(
      releasedToExternalBuildDetails,
      releasedToExternalBuildDetails.copy(id = "id127"),
      releasedToExternalBuildDetails.copy(id = "id128"),
      releasedToExternalBuildDetails.copy(id = "id129"),
    )
    val result = BuildOutput.findPreviousThreeBetaVersions(allBetas, buildDetails)
    assert(result.isFailure)
  }

}

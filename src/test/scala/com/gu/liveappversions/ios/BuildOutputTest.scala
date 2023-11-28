package com.gu.liveappversions.ios

import java.time.ZonedDateTime

import com.gu.appstoreconnectapi.Conversion.LiveAppBeta
import org.scalatest.FunSuite

class BuildOutputTest extends FunSuite {

  val now = ZonedDateTime.now()

  val releasedToExternalBeta = LiveAppBeta("12349", "id123", now, "IN_BETA_TESTING", "IN_BETA_TESTING")
  val unreleasedToExternalBeta = LiveAppBeta("22349", "id223", now, "IN_BETA_TESTING", "IN_REVIEW")

  test("findLatestBuildsWithExternalTesters should correctly identify the 4 most recent beta versions from Apple's response") {

    val betaBuilds = List(
      releasedToExternalBeta,
      releasedToExternalBeta.copy("12348"),
      releasedToExternalBeta.copy("12347"),
      releasedToExternalBeta.copy("12346"),
      releasedToExternalBeta.copy("12345"))

    val result = BuildOutput.findLatestBuildsWithExternalTesters(betaBuilds)

    assert(result.get.latestReleasedBuild.version === "12349")
    assert(result.get.previouslyReleasedBuilds.map(_.version) === List("12348", "12347", "12346"))

  }

  test("findLatestBuildsWithExternalTesters should ignore betas which have NOT been released to external testers") {
    val betaBuilds = List(
      unreleasedToExternalBeta,
      releasedToExternalBeta,
      releasedToExternalBeta.copy("12348"),
      releasedToExternalBeta.copy("12347"),
      releasedToExternalBeta.copy("12346"))
    val result = BuildOutput.findLatestBuildsWithExternalTesters(betaBuilds)

    assert(result.get.latestReleasedBuild.version === "12349")
    assert(result.get.previouslyReleasedBuilds.map(_.version) === List("12348", "12347", "12346"))

  }

  test("findLatestBuildsWithExternalTesters should fail if there are less than 4 recent beta versions") {
    val betaBuilds = List(
      releasedToExternalBeta,
      releasedToExternalBeta.copy("12348"),
      releasedToExternalBeta.copy("12347"))
    val result = BuildOutput.findLatestBuildsWithExternalTesters(betaBuilds)
    assert(result.isFailure)
  }

}

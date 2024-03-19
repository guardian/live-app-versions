package com.gu.appstoreconnectapi

import java.time.ZonedDateTime
import com.gu.appstoreconnectapi.AppStoreConnectApi.{ BetaBuildAttributes, BetaBuildDetails, BuildAttributes, BuildDetails, BuildsResponse }
import com.gu.appstoreconnectapi.Conversion.LiveAppBeta

import scala.util.{ Failure, Success }
import org.scalatest.funsuite.AnyFunSuite

class ConversionTest extends AnyFunSuite {

  val now = ZonedDateTime.now()
  val buildDetails = BuildDetails("id123", BuildAttributes("12345", now))
  val betaBuildDetails = BetaBuildDetails("id123", BetaBuildAttributes(internalBuildState = "INTERNAL", externalBuildState = "EXTERNAL"))

  val allBuildDetails = List(
    buildDetails,
    buildDetails.copy(id = "id124").copy(attributes = BuildAttributes("12344", now)),
    buildDetails.copy(id = "id125").copy(attributes = BuildAttributes("12343", now)),
    buildDetails.copy(id = "id126").copy(attributes = BuildAttributes("12342", now)),
    buildDetails.copy(id = "id127").copy(attributes = BuildAttributes("12341", now)))
  val allBetaBuildDetails = List(
    betaBuildDetails,
    betaBuildDetails.copy(id = "id124"),
    betaBuildDetails.copy(id = "id125"),
    betaBuildDetails.copy(id = "id126"),
    betaBuildDetails.copy(id = "id127"))

  test("combineModels should produce a list of LiveAppBetas from a valid App Store Connect response") {
    val buildsResponse = BuildsResponse(allBuildDetails, allBetaBuildDetails)
    val result = Conversion.combineBuildsResponseModels(buildsResponse)
    val expectedResults = List(
      LiveAppBeta("12345", "id123", now, "INTERNAL", "EXTERNAL"),
      LiveAppBeta("12344", "id124", now, "INTERNAL", "EXTERNAL"),
      LiveAppBeta("12343", "id125", now, "INTERNAL", "EXTERNAL"),
      LiveAppBeta("12342", "id126", now, "INTERNAL", "EXTERNAL"),
      LiveAppBeta("12341", "id127", now, "INTERNAL", "EXTERNAL"))
    assert(result == Success(expectedResults))
  }

  test("combineModels should fail if there are objects missing from the App Store Connect response") {
    val buildsResponse = BuildsResponse(allBuildDetails.drop(1), allBetaBuildDetails)
    val result = Conversion.combineBuildsResponseModels(buildsResponse)
    assert(result.isFailure)
  }

  test("combineModels should fail if the ids in the App Store Connect response do not match") {
    val buildsResponse = BuildsResponse(List(buildDetails), List(betaBuildDetails.copy(id = "fakeId")))
    val result = Conversion.combineBuildsResponseModels(buildsResponse)
    assert(result.isFailure)
  }

}

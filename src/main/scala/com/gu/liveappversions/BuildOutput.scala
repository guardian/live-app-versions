package com.gu.liveappversions

import com.gu.liveappversions.AppStoreConnectApi.{ BetaBuildDetails, BuildAttributes, BuildDetails, BuildsResponse }
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.generic.semiauto._

import scala.util.{ Failure, Success, Try }

case class BuildOutput(latestReleasedBuild: BuildAttributes, previouslyReleasedBuilds: List[BuildAttributes])

object BuildOutput {

  implicit val buildOutputEncoder: Encoder[BuildOutput] = deriveEncoder[BuildOutput]

  def buildAttributesForBuildDetails(buildId: String, buildsDetails: List[BuildDetails]): Option[BuildAttributes] = {
    buildsDetails.find(_.id == buildId).map(_.attributes)
  }

  def findPreviousThreeBetaVersions(allBetas: List[BetaBuildDetails], buildsDetails: List[BuildDetails]): Try[List[BuildAttributes]] = {
    val attemptToFindPreviousThreeBetas = allBetas.slice(1, 4).flatMap { beta =>
      buildAttributesForBuildDetails(beta.id, buildsDetails)
    }
    if (attemptToFindPreviousThreeBetas.size != 3) {
      Failure(new RuntimeException(s"Expected to find at least three previous betas but found ${attemptToFindPreviousThreeBetas.size}"))
    } else {
      Success(attemptToFindPreviousThreeBetas)
    }
  }

  def findLatestBuildsWithExternalTesters(buildsResponse: BuildsResponse): Try[BuildOutput] = {
    val betasWithExternalTesters = buildsResponse.included.filter(_.attributes.externalBuildState == "IN_BETA_TESTING")
    for {
      latestBetaWithExternalTesters <- Try { buildAttributesForBuildDetails(betasWithExternalTesters.head.id, buildsResponse.data).get }
      previousBetasWithExternalTesters <- findPreviousThreeBetaVersions(betasWithExternalTesters, buildsResponse.data)
    } yield BuildOutput(latestBetaWithExternalTesters, previousBetasWithExternalTesters)
  }

  def fromAppStoreConnectResponse(buildsResponse: BuildsResponse): Try[BuildOutput] = {
    findLatestBuildsWithExternalTesters(buildsResponse)
  }

}

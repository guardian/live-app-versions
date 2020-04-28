package com.gu.liveappversions

import com.gu.appstoreconnectapi.Conversion.LiveAppBeta
import com.gu.liveappversions.Lambda.logger
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{ Encoder, HCursor, Json }

import scala.util.{ Failure, Success, Try }

case class BuildOutput(latestReleasedBuild: LiveAppBeta, previouslyReleasedBuilds: List[LiveAppBeta])

object BuildOutput {

  implicit val buildOutputEncoder: Encoder[BuildOutput] = deriveEncoder[BuildOutput]

  implicit val liveAppBetaEncoder: Encoder[LiveAppBeta] = new Encoder[LiveAppBeta] {
    final def apply(beta: LiveAppBeta): Json = Json.obj(
      ("version", Json.fromString(beta.version)),
      ("uploadedDate", Json.fromString(beta.uploadedDate.toString)))
  }

  def findPreviousThreeBetaVersions(betas: List[LiveAppBeta]): Try[List[LiveAppBeta]] = {
    val attemptToFindPreviousThreeBetas = betas.slice(1, 4)
    if (attemptToFindPreviousThreeBetas.size != 3) {
      Failure(new RuntimeException(s"Expected to find at least three previous betas but found ${attemptToFindPreviousThreeBetas.size}"))
    } else {
      Success(attemptToFindPreviousThreeBetas)
    }
  }

  def findLatestBuildsWithExternalTesters(betas: List[LiveAppBeta]): Try[BuildOutput] = {
    val betasWithExternalTesters = betas.filter(_.externalBuildState == "IN_BETA_TESTING")
    for {
      latestBetaWithExternalTesters <- Try { betasWithExternalTesters.head }
      previousBetasWithExternalTesters <- findPreviousThreeBetaVersions(betasWithExternalTesters)
    } yield {
      logger.info(s"The latest iOS beta with external beta testers is: ${latestBetaWithExternalTesters}. Previous versions are: ${previousBetasWithExternalTesters}")
      BuildOutput(latestBetaWithExternalTesters, previousBetasWithExternalTesters)
    }
  }

}

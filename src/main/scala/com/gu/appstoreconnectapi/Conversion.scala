package com.gu.appstoreconnectapi

import java.time.ZonedDateTime

import com.gu.appstoreconnectapi.AppStoreConnectApi.BuildsResponse

import scala.util.{Failure, Success, Try}

object Conversion {

  case class LiveAppBeta(version: String, buildId: String, uploadedDate: ZonedDateTime, internalBuildState: String, externalBuildState: String)

  case object CombinedResponseException extends Throwable

  // App Store Connect API provides two different objects which can be matched by id
  // Since we need data from both objects we combine them into a single case class here
  // to avoid scattering this matching logic throughout the codebase
  def combineModels(buildsResponse: BuildsResponse): Try[List[LiveAppBeta]] = {
    val buildDetailsIds = buildsResponse.data.map(_.id)
    val betaBuildDetailsIds = buildsResponse.included.map(_.id)
    if (buildDetailsIds != betaBuildDetailsIds) {
      Failure(CombinedResponseException)
    } else {
      val liveAppBetas = buildsResponse.data.map {
        buildDetails =>
          val betaBuildDetails = buildsResponse.included.find(_.id == buildDetails.id).get
          LiveAppBeta(
            version = buildDetails.attributes.version,
            buildId = buildDetails.id,
            uploadedDate = buildDetails.attributes.uploadedDate,
            internalBuildState = betaBuildDetails.attributes.internalBuildState,
            externalBuildState = betaBuildDetails.attributes.externalBuildState,
          )
      }
      Success(liveAppBetas)
    }
  }

}

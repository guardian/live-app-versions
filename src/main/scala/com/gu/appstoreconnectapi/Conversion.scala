package com.gu.appstoreconnectapi

import java.time.ZonedDateTime

import com.gu.appstoreconnectapi.AppStoreConnectApi.{ AppStoreVersionsResponse, BuildsResponse }

import scala.util.{ Failure, Success, Try }

object Conversion {

  case class LiveAppBeta(version: String, buildId: String, uploadedDate: ZonedDateTime, internalBuildState: String, externalBuildState: String)
  case class LiveAppProduction(versionString: String, version: String, appStoreVersionState: String)

  case object CombinedResponseException extends Throwable

  // App Store Connect API provides two different objects which can be matched by id
  // Since we need data from both objects we combine them into a single case class here
  // to avoid scattering this matching logic throughout the codebase
  def combineBuildsResponseModels(buildsResponse: BuildsResponse): Try[List[LiveAppBeta]] = {
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
            externalBuildState = betaBuildDetails.attributes.externalBuildState)
      }
      Success(liveAppBetas)
    }
  }

  def combineAppStoreVersionsResponseModels(appStoreVersionsResponse: AppStoreVersionsResponse): Try[List[LiveAppProduction]] = {
    val appStoreVersionsWithBuild = appStoreVersionsResponse.data.filter(_.relationships.build.data.isDefined)
    val appStoreVersionsIds = appStoreVersionsWithBuild.map(_.id)
    val buildDetailsIds = appStoreVersionsResponse.data.flatMap(_.relationships.build.data.map(_.id))
    if (appStoreVersionsIds.size != buildDetailsIds.size) {
      Failure(CombinedResponseException)
    } else {
      val productionVersions = appStoreVersionsWithBuild.map {
        appStoreVersion =>
          val buildDetails = appStoreVersionsResponse.included.find(_.id == appStoreVersion.relationships.build.data.map(_.id).get).get
          LiveAppProduction(
            appStoreVersion.attributes.versionString,
            buildDetails.attributes.version,
            appStoreVersion.attributes.appStoreState)
      }
      Success(productionVersions)
    }
  }

}

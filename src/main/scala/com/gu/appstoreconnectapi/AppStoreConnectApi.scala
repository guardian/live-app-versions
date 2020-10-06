package com.gu.appstoreconnectapi

import java.time.{ ZoneOffset, ZonedDateTime }

import com.gu.appstoreconnectapi.Conversion.{ LiveAppBeta, LiveAppProduction, combineBuildsResponseModels, combineAppStoreVersionsResponseModels }
import com.gu.config.Config.{ AppStoreConnectConfig, ExternalTesterConfig }
import com.gu.okhttp.SharedClient
import io.circe.Decoder.Result
import io.circe.parser.decode
import io.circe.{ Decoder, HCursor }
import okhttp3.{ MediaType, Request, RequestBody }
import io.circe.parser._
import io.circe.generic.auto._
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.Try

object AppStoreConnectApi {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  implicit val decodeBuildAttributes: Decoder[BuildAttributes] = new Decoder[BuildAttributes] {
    override def apply(c: HCursor): Result[BuildAttributes] = for {
      version <- c.downField("version").as[String]
      uploadedDate <- c.downField("uploadedDate").as[ZonedDateTime]
    } yield {
      BuildAttributes(version, uploadedDate.withZoneSameInstant(ZoneOffset.UTC))
    }
  }

  case class BuildAttributes(version: String, uploadedDate: ZonedDateTime)
  case class BuildDetails(id: String, attributes: BuildAttributes)
  case class BetaBuildAttributes(internalBuildState: String, externalBuildState: String)
  case class BetaBuildDetails(id: String, attributes: BetaBuildAttributes)
  case class BuildsResponse(data: List[BuildDetails], included: List[BetaBuildDetails])
  case class AppStoreVersionAttributes(versionString: String, appStoreState: String, earliestReleaseDate: ZonedDateTime, releaseType: String)
  case class AppStoreVersion(attributes: AppStoreVersionAttributes)
  case class AppStoreVersionsResponse(data: List[AppStoreVersion], included: List[BuildDetails])

  val appStoreConnectBaseUrl = "https://api.appstoreconnect.apple.com/v1"

  def getLatestBetaBuilds(token: String, appStoreConnectConfig: AppStoreConnectConfig): Try[List[LiveAppBeta]] = {
    val buildsQuery = s"/builds?limit=20&sort=-version&include=buildBetaDetail&filter[app]=${appStoreConnectConfig.appleAppId}"
    val request = new Request.Builder()
      .url(s"$appStoreConnectBaseUrl$buildsQuery")
      .addHeader("Authorization", s"Bearer $token")
      .build
    for {
      httpResponse <- Try(SharedClient.client.newCall(request).execute)
      bodyAsString <- SharedClient.getResponseBodyIfSuccessful("App Store Connect API", httpResponse)
      buildsResponse <- decode[BuildsResponse](bodyAsString).toTry
      liveAppBetas <- combineBuildsResponseModels(buildsResponse)
    } yield liveAppBetas
  }

  def getLatestProductionBuild(token: String, appStoreConnectConfig: AppStoreConnectConfig): Try[LiveAppProduction] = {
    val buildsQuery = s"/apps/${appStoreConnectConfig.appleAppId}/appStoreVersions?filter[appStoreState]=READY_FOR_SALE&include=build"
    val request = new Request.Builder()
      .url(s"$appStoreConnectBaseUrl$buildsQuery")
      .addHeader("Authorization", s"Bearer $token")
      .build
    for {
      httpResponse <- Try(SharedClient.client.newCall(request).execute)
      bodyAsString <- SharedClient.getResponseBodyIfSuccessful("App Store Connect API", httpResponse)
      appStoreVersionsResponse <- decode[AppStoreVersionsResponse](bodyAsString).toTry
      latestProductionRelease <- combineAppStoreVersionsResponseModels(appStoreVersionsResponse)
    } yield {
      logger.info(s"The latest production release is: $latestProductionRelease")
      latestProductionRelease
    }
  }

  def submitForBetaTesting(token: String, buildId: String): Try[Unit] = {
    val url = s"$appStoreConnectBaseUrl/betaAppReviewSubmissions"
    val body = s"""
                  |{
                  |  "data": {
                  |    "relationships": {
                  |      "build": {
                  |        "data": {
                  |          "id": "$buildId",
                  |          "type": "builds"
                  |        }
                  |      }
                  |    },
                  |    "type": "betaAppReviewSubmissions"
                  |  }
                  |}
                  |""".stripMargin
    val request = new Request.Builder()
      .url(url)
      .addHeader("Authorization", s"Bearer $token")
      .post(RequestBody.create(body, MediaType.get("application/json; charset=utf-8")))
      .build
    for {
      httpResponse <- Try(SharedClient.client.newCall(request).execute)
      _ <- SharedClient.getResponseBodyIfSuccessful("App Store Connect API", httpResponse)
    } yield {
      logger.info(s"Successfully submitted build for beta review")
    }
  }

  def distributeToExternalTesters(token: String, buildId: String, externalTesterConfig: ExternalTesterConfig): Try[Unit] = {
    val url = s"$appStoreConnectBaseUrl/builds/$buildId/relationships/betaGroups"
    val body = s"""
                  |{
                  |  "data": [
                  |     {
                  |       "id": "${externalTesterConfig.group1.id}",
                  |         "type": "betaGroups"
                  |     },
                  |     {
                  |       "id": "${externalTesterConfig.group2.id}",
                  |         "type": "betaGroups"
                  |     }
                  |  ]
                  |}
                  |""".stripMargin
    val request = new Request.Builder()
      .url(url)
      .addHeader("Authorization", s"Bearer $token")
      .post(RequestBody.create(body, MediaType.get("application/json; charset=utf-8")))
      .build
    for {
      httpResponse <- Try(SharedClient.client.newCall(request).execute)
      _ <- SharedClient.getResponseBodyIfSuccessful("App Store Connect API", httpResponse)
    } yield {
      logger.info(s"Successfully distributed build to ${externalTesterConfig.group1} and ${externalTesterConfig.group2}")
    }
  }

}

package com.gu.liveappversions

import com.gu.liveappversions.Config.AppStoreConnectConfig
import okhttp3.{ OkHttpClient, Request }
import io.circe.parser._
import io.circe.generic.auto._

import scala.util.Try

object AppStoreConnectApi {

  case class BuildAttributes(version: String)
  case class BuildDetails(id: String, attributes: BuildAttributes)
  case class BetaBuildAttributes(externalBuildState: String)
  case class BetaBuildDetails(id: String, attributes: BetaBuildAttributes)

  case class BuildsResponse(data: List[BuildDetails], included: List[BetaBuildDetails])

  val client = new OkHttpClient
  val appStoreConnectBaseUrl = "https://api.appstoreconnect.apple.com/v1"

  def getLatestBetaBuilds(token: String, appStoreConnectConfig: AppStoreConnectConfig): Try[BuildsResponse] = {
    val buildsQuery = s"/builds?limit=20&sort=-version&include=buildBetaDetail&filter[app]=${appStoreConnectConfig.appleAppId}"
    val request = new Request.Builder()
      .url(s"$appStoreConnectBaseUrl$buildsQuery")
      .addHeader("Authorization", s"Bearer $token")
      .build
    for {
      httpResponse <- Try(client.newCall(request).execute)
      buildsResponse <- decode[BuildsResponse](httpResponse.body().string()).toTry
    } yield buildsResponse
  }

}

package com.gu.liveappversions

import java.time.{ ZoneOffset, ZonedDateTime }

import com.gu.liveappversions.Config.AppStoreConnectConfig
import io.circe.Decoder.Result
import io.circe.{ Decoder, HCursor }
import okhttp3.{ OkHttpClient, Request }
import io.circe.parser._
import io.circe.generic.auto._

import scala.util.Try

object AppStoreConnectApi {

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

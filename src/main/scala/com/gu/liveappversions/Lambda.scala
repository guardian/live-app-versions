package com.gu.liveappversions

import com.amazonaws.services.lambda.runtime.Context
import com.gu.liveappversions.Config.{ AppStoreConnectConfig, Env }
import org.slf4j.{ Logger, LoggerFactory }
import okhttp3.OkHttpClient
import okhttp3.Request
import io.circe.parser._

import scala.util.Try

object AppStoreConnectApi {

  import io.circe.generic.auto._

  case class BuildAttributes(version: String)
  case class BuildDetails(id: String, attributes: BuildAttributes)
  case class BetaBuildAttributes(externalBuildState: String)
  case class BetaBuildDetails(id: String, attributes: BetaBuildAttributes)

  case class BuildsResponse(data: List[BuildDetails], included: List[BetaBuildDetails])

  val client = new OkHttpClient
  val appStoreConnectBaseUrl = "https://api.appstoreconnect.apple.com/v1"

  def findLatestBuildWithExternalTesters(buildsResponse: BuildsResponse): Option[BuildDetails] = {
    val latestBetaWithExternalTesters = buildsResponse.included.find(_.attributes.externalBuildState == "IN_BETA_TESTING")
    latestBetaWithExternalTesters.flatMap(betaBuild => buildsResponse.data.find(_.id == betaBuild.id))
  }

  def latestBuildWithExternalTesters(token: String): Option[BuildDetails] = {
    val buildsQuery = "/builds?limit=20&sort=-version&include=buildBetaDetail&filter[app]=409128287"
    val request = new Request.Builder()
      .url(s"$appStoreConnectBaseUrl$buildsQuery")
      .addHeader("Authorization", s"Bearer $token")
      .build
    for {
      response <- Try(client.newCall(request).execute).toOption
      parseAttempt <- decode[BuildsResponse](response.body().string()).toOption
      build <- findLatestBuildWithExternalTesters(parseAttempt)
    } yield build
  }

}

object Lambda {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def handler(context: Context): Unit = {
    val env = Env()
    logger.info(s"Starting $env")
    process()
  }

  def process(): Unit = {
    val token = JwtTokenBuilder.buildToken(AppStoreConnectConfig())
    val latestBuild = AppStoreConnectApi.latestBuildWithExternalTesters(token)
    println(latestBuild)
  }

}

object TestIt {
  def main(args: Array[String]): Unit = {
    Lambda.process()
  }
}

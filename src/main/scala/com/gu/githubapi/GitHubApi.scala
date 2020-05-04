package com.gu.githubapi

import com.gu.config.Config.GitHubConfig
import com.gu.githubapi.Conversion.{ RunningLiveAppDeployment, runningLiveAppDeployments }
import com.gu.okhttp.SharedClient
import io.circe.Decoder
import io.circe.parser._
import io.circe.generic.auto._
import okhttp3.{ MediaType, Request, RequestBody }

import scala.util.Try

object GitHubApi {

  val graphQlApiUrl = "https://api.github.com/graphql"
  val restApiUrl = "https://api.github.com"

  case class GitHubApiException(message: String) extends Throwable(message: String)

  case class Node(node: Deployment)
  case class Deployment(databaseId: Int, environment: String, state: String, latestStatus: Option[LatestStatus])
  case class LatestStatus(description: Option[String])

  val deploymentsDecoder = Decoder[List[Node]].prepare(
    _.downField("data")
      .downField("repository")
      .downField("deployments")
      .downField("edges"))

  def extractDeployments(responseBody: String) = {
    val nodes = decode(responseBody)(deploymentsDecoder)
    nodes.map(_.map(_.node))
  }

  def gitHubPostRequest(url: String, body: String, gitHubConfig: GitHubConfig): Request = {
    new Request.Builder()
      .url(url)
      .addHeader("Authorization", s"token ${gitHubConfig.token}")
      .post(RequestBody.create(body, MediaType.get("application/json; charset=utf-8")))
      .build
  }

  def getRunningDeployments(gitHubConfig: GitHubConfig): Try[List[RunningLiveAppDeployment]] = {
    val query =
      """
    |{
    |	"query": "query { repository(owner:\"guardian\", name:\"ios-live\") { deployments(last: 3) { edges { node { databaseId, createdAt, environment, state, payload, latestStatus { createdAt, description  } } } } } }"
    |}
    |""".stripMargin
    for {
      httpResponse <- Try(SharedClient.client.newCall(gitHubPostRequest(graphQlApiUrl, query, gitHubConfig)).execute)
      bodyAsString <- SharedClient.getResponseBodyIfSuccessful("GitHub API", httpResponse)
      deployments <- extractDeployments(bodyAsString).toTry
    } yield {
      runningLiveAppDeployments(deployments)
    }
  }

}

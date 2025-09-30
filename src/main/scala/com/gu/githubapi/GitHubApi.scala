package com.gu.githubapi

import java.time.ZonedDateTime
import com.gu.config.Config.GitHubConfig
import com.gu.githubapi.Conversion.{ FailedLiveAppDeployment, LiveAppDeployment, RunningLiveAppDeployment, failedLiveAppDeployments, runningLiveAppDeployments }
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
  case class Deployment(databaseId: Long, environment: String, state: String, latestStatus: Option[LatestStatus], createdAt: ZonedDateTime)
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
      .addHeader("Authorization", s"Bearer ${gitHubConfig.token}")
      .post(RequestBody.create(body, MediaType.get("application/json; charset=utf-8")))
      .build
  }

  def getDeployments(gitHubConfig: GitHubConfig, state: String): Try[List[Deployment]] = {
    val query =
      """
        |{
        |	"query": "query { repository(owner:\"guardian\", name:\"ios-live\") { deployments(last: 10) { edges { node { databaseId, createdAt, environment, state, latestStatus { createdAt, description  } } } } } }"
        |}
        |""".stripMargin
    for {
      httpResponse <- Try(SharedClient.client.newCall(gitHubPostRequest(graphQlApiUrl, query, gitHubConfig)).execute)
      bodyAsString <- SharedClient.getResponseBodyIfSuccessful("GitHub API", httpResponse)
      deployments <- extractDeployments(bodyAsString).toTry
    } yield {
      deployments.filter(_.state == state)
    }
  }

  def getRunningDeployments(gitHubConfig: GitHubConfig): Try[List[RunningLiveAppDeployment]] = for {
    deployments <- getDeployments(gitHubConfig, "PENDING")
  } yield {
    runningLiveAppDeployments(deployments)
  }

  def getFailedDeployments(gitHubConfig: GitHubConfig): Try[List[FailedLiveAppDeployment]] = for {
    deployments <- getDeployments(gitHubConfig, "FAILURE")
  } yield {
    failedLiveAppDeployments(deployments)
  }

  def markDeploymentAsSuccess(gitHubConfig: GitHubConfig, deployment: LiveAppDeployment): Try[Unit] = {
    markDeploymentAsFinished(gitHubConfig, deployment, "success")
  }

  def markDeploymentAsFailure(gitHubConfig: GitHubConfig, deployment: LiveAppDeployment): Try[Unit] = {
    markDeploymentAsFinished(gitHubConfig, deployment, "failure")
  }

  def markDeploymentAsFinished(gitHubConfig: GitHubConfig, deployment: LiveAppDeployment, finishedState: String): Try[Unit] = {
    val url = s"$restApiUrl/repos/guardian/ios-live/deployments/${deployment.gitHubDatabaseId.toString}/statuses"
    val body =
      s"""
         |{
         |  "state": "$finishedState",
         |  "description": "${deployment.version}"
         |}
         |""".stripMargin
    for {
      httpResponse <- Try(SharedClient.client.newCall(gitHubPostRequest(url, body, gitHubConfig)).execute)
      _ <- SharedClient.getResponseBodyIfSuccessful("GitHub API", httpResponse)
    } yield {
      ()
    }
  }
}

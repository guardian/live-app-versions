package com.gu.githubapi

import java.time.ZonedDateTime

import com.gu.githubapi.GitHubApi.Deployment

object Conversion {

  case class RunningLiveAppDeployment(version: String, environment: String, gitHubDatabaseId: Long, createdAt: ZonedDateTime)

  def runningLiveAppDeployments(gitHubDeployments: List[Deployment]): List[RunningLiveAppDeployment] = {
    gitHubDeployments
      .filter(deployment => deployment.state == "PENDING" && deployment.latestStatus.flatMap(_.description).isDefined)
      .map(deployment => RunningLiveAppDeployment(deployment.latestStatus.get.description.get, deployment.environment, deployment.databaseId, deployment.createdAt))
  }

}

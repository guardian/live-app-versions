package com.gu.githubapi

import java.time.ZonedDateTime

import com.gu.githubapi.GitHubApi.Deployment

object Conversion {

  sealed trait LiveAppDeployment {
    def version: String
    def environment: String
    def gitHubDatabaseId: Long
    def createdAt: ZonedDateTime
  }

  case class RunningLiveAppDeployment(version: String, environment: String, gitHubDatabaseId: Long, createdAt: ZonedDateTime) extends LiveAppDeployment

  case class FailedLiveAppDeployment(version: String, environment: String, gitHubDatabaseId: Long, createdAt: ZonedDateTime) extends LiveAppDeployment

  private def convertDeployments[T <: LiveAppDeployment](gitHubDeployments: List[Deployment], state: String, constructor: (String, String, Long, ZonedDateTime) => T): List[T] = {
    gitHubDeployments
      .filter(deployment => deployment.state == state && deployment.latestStatus.flatMap(_.description).isDefined)
      .map(deployment => constructor(
        deployment.latestStatus.get.description.get,
        deployment.environment,
        deployment.databaseId,
        deployment.createdAt))
  }

  def runningLiveAppDeployments(gitHubDeployments: List[Deployment]): List[RunningLiveAppDeployment] = {
    convertDeployments(gitHubDeployments, "PENDING", RunningLiveAppDeployment.apply)
  }

  def failedLiveAppDeployments(gitHubDeployments: List[Deployment]): List[FailedLiveAppDeployment] = {
    convertDeployments(gitHubDeployments, "FAILURE", FailedLiveAppDeployment.apply)
  }
}

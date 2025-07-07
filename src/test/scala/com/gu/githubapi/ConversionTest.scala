package com.gu.githubapi

import java.time.ZonedDateTime

import com.gu.githubapi.Conversion.RunningLiveAppDeployment
import com.gu.githubapi.GitHubApi.{ Deployment, LatestStatus }
import org.scalatest.funsuite.AnyFunSuite

class ConversionTest extends AnyFunSuite {

  val now = ZonedDateTime.now()

  val deployment = Deployment(
    123,
    "external-beta",
    "PENDING",
    Some(LatestStatus(Some("12345"))),
    now)

  val failedDeployment = Deployment(
    123,
    "production",
    "FAILURE",
    Some(LatestStatus(Some("12345"))),
    now)

  test("runningLiveAppDeployments should collect pending deployments with version info") {
    val result = Conversion.runningLiveAppDeployments(List(deployment))
    val expected = List(RunningLiveAppDeployment("12345", "external-beta", 123, now))
    assert(result == expected)
  }

  test("runningLiveAppDeployments should discard completed deployments") {
    val completedDeployment = deployment.copy(state = "SUCCESSFUL")
    val result = Conversion.runningLiveAppDeployments(List(completedDeployment))
    val expected = List()
    assert(result == expected)
  }

  test("runningLiveAppDeployments should discard pending deployments which do not contain version info") {
    val deploymentOnCreation = deployment.copy(latestStatus = None)
    val deploymentWithUnexpectedStatusUpdate = deployment.copy(latestStatus = Some(LatestStatus(None)))
    val result = Conversion.runningLiveAppDeployments(List(deploymentOnCreation, deploymentWithUnexpectedStatusUpdate))
    val expected = List()
    assert(result == expected)
  }

  test("failedLiveAppDeployments should collect failed deployments with version info") {
    val result = Conversion.failedLiveAppDeployments(List(failedDeployment))
    val expected = List(Conversion.FailedLiveAppDeployment("12345", "production", 123, now))
    assert(result == expected)
  }
}

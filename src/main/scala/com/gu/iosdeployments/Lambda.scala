package com.gu.iosdeployments

import java.time.ZonedDateTime

import com.amazonaws.services.lambda.runtime.Context
import com.gu.appstoreconnectapi.Conversion.{LiveAppBeta, LiveAppProduction}
import com.gu.appstoreconnectapi.{AppStoreConnectApi, JwtTokenBuilder}
import com.gu.config.Config
import com.gu.config.Config.{AppStoreConnectConfig, Env, GitHubConfig}
import com.gu.githubapi.Conversion.RunningLiveAppDeployment
import com.gu.githubapi.GitHubApi
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

object Lambda {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def handler(context: Context): Unit = {
    val env = Env()
    logger.info(s"Starting $env")
    process(env)
  }

  def handleBetaDeployment(env: Env, maybeDeployment: Option[RunningLiveAppDeployment], latestBetas: List[LiveAppBeta], appStoreConnectToken: String, gitHubConfig: GitHubConfig): Try[Unit] = {

    val externalTesterConfig = if (env.stage == "PROD") {
      Config.externalTesterConfigForProd
    } else {
      Config.externalTesterConfigForTesting
    }

    maybeDeployment match {
      case Some(runningDeployment) =>
        val attemptToFindBeta = latestBetas.find(_.version == runningDeployment.version)
        (runningDeployment.environment, attemptToFindBeta) match {
          case ("internal-beta", Some(LiveAppBeta(_, _, _, "IN_BETA_TESTING", _))) =>
            logger.info(s"Internal beta deployment for ${runningDeployment.version} is complete...")
            GitHubApi.markDeploymentAsSuccess(gitHubConfig, runningDeployment)
          case ("external-beta", Some(LiveAppBeta(_, _, _, _, "IN_BETA_TESTING"))) =>
            logger.info(s"External beta deployment for ${runningDeployment.version} is complete...")
            GitHubApi.markDeploymentAsSuccess(gitHubConfig, runningDeployment)
          case ("external-beta", Some(build @ LiveAppBeta(_, _, _, "IN_BETA_TESTING", "READY_FOR_BETA_SUBMISSION"))) =>
            logger.info(s"External beta deployment for ${runningDeployment.version} can now be submitted for review...")
            AppStoreConnectApi.submitForBetaTesting(appStoreConnectToken, build.buildId)
          case ("external-beta", Some(build @ LiveAppBeta(_, _, _, _, "BETA_APPROVED"))) =>
            logger.info(s"External beta deployment for ${runningDeployment.version} can now be distributed to users...")
            AppStoreConnectApi.distributeToExternalTesters(appStoreConnectToken, build.buildId, externalTesterConfig)
          case ("external-beta", Some(build @ LiveAppBeta(_, _, _, _, "BETA_REJECTED"))) =>
            logger.info(s"External beta for ${runningDeployment.version} was rejected.")
            GitHubApi.markDeploymentAsFailure(gitHubConfig, runningDeployment)
          case (_, Some(LiveAppBeta(_, _, _, "EXPIRED", "EXPIRED"))) =>
            logger.info(s"Beta deployment for ${runningDeployment.version} was (presumably) successful, but beta build was expired before deployment completed...")
            GitHubApi.markDeploymentAsSuccess(gitHubConfig, runningDeployment)
          case (_, None) =>
            notPresentInAppStoreConnect(runningDeployment, gitHubConfig)
          case _ =>
            Try(logger.info(s"No action was required for beta deployment ${runningDeployment.version}. Full details are: $attemptToFindBeta"))
        }
      case None =>
        Try(logger.info("No running beta deployments found."))
    }
  }

  def handleProductionDeployment(maybeDeployment: Option[RunningLiveAppDeployment], latestProductionVersions: List[LiveAppProduction], gitHubConfig: GitHubConfig): Try[Unit] = maybeDeployment match {
    case Some(productionDeployment) =>
      val attemptToFindProductionVersion = latestProductionVersions.find(_.version == productionDeployment.version)
      attemptToFindProductionVersion match {
        case Some(LiveAppProduction(_, _, "READY_FOR_SALE")) =>
          logger.info(s"Production deployment for version ${productionDeployment.version} is complete...")
          GitHubApi.markDeploymentAsSuccess(gitHubConfig, productionDeployment)
        // There are a few different ways a build can be rejected, see: https://developer.apple.com/documentation/appstoreconnectapi/appstoreversionstate
        case Some(LiveAppProduction(_, _, status)) if status.contains("REJECTED") =>
          logger.info(s"Production deployment for version ${productionDeployment.version} has been rejected...")
          GitHubApi.markDeploymentAsFailure(gitHubConfig, productionDeployment)
        case Some(liveAppProduction) =>
          Try(logger.info(s"No action required for production deployment. Full details are: ${liveAppProduction}"))
        case None =>
          notPresentInAppStoreConnect(productionDeployment, gitHubConfig)
      }
    case None =>
      Try(logger.info("No running production deployments found."))
  }

  def notPresentInAppStoreConnect(runningDeployment: RunningLiveAppDeployment, gitHubConfig: GitHubConfig) = {

    def olderThanOneHour(deployment: RunningLiveAppDeployment): Boolean = {
      val oneHourAgo = ZonedDateTime.now().minusHours(1)
      deployment.createdAt.isBefore(oneHourAgo)
    }

    if (olderThanOneHour(runningDeployment)) {
      logger.warn(s"Unable to correlate running ${runningDeployment.environment} deployment for version ${runningDeployment.version} with App Store Connect response after one hour. Giving up...")
      GitHubApi.markDeploymentAsFailure(gitHubConfig, runningDeployment)
    } else {
      Try(logger.info(s"Found running ${runningDeployment.environment} deployment for version ${runningDeployment.version}. Waiting for App Store Connect response to catch-up..."))
    }

  }

  def process(env: Env): Unit = {
    logger.info("Loading configuration...")
    val appStoreConnectConfig = AppStoreConnectConfig(env)
    val appStoreConnectToken = JwtTokenBuilder.buildToken(appStoreConnectConfig)
    val gitHubConfig = GitHubConfig(env)
    logger.info("Successfully loaded configuration...")
    val result = for {
      runningDeployments <- GitHubApi.getRunningDeployments(gitHubConfig)
      appStoreConnectBetaBuilds <- AppStoreConnectApi.getLatestBetaBuilds(appStoreConnectToken, appStoreConnectConfig)
      appStoreConnectProductionBuild <- AppStoreConnectApi.getLatestProductionBuilds(appStoreConnectToken, appStoreConnectConfig)
      maybeBetaDeployment = runningDeployments.find(_.environment.contains("beta"))
      maybeProductionDeployment = runningDeployments.find(_.environment == "production")
      handleBeta <- handleBetaDeployment(env, maybeBetaDeployment, appStoreConnectBetaBuilds, appStoreConnectToken, gitHubConfig)
      handleProduction <- handleProductionDeployment(maybeProductionDeployment, appStoreConnectProductionBuild, gitHubConfig)
    } yield {
      handleProduction
    }

    result match {
      case Success(_) => logger.info("Successfully checked/updated deployment status")
      case Failure(exception) => logger.error(s"Failed to check or update deployment status due to: ${exception}", exception)
    }

  }
}

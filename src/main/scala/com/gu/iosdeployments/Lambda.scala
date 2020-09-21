package com.gu.iosdeployments

import com.amazonaws.services.lambda.runtime.Context
import com.gu.appstoreconnectapi.Conversion.LiveAppBeta
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
          case ("external-beta", Some(build@LiveAppBeta(_, _, _, "IN_BETA_TESTING", "READY_FOR_BETA_SUBMISSION"))) =>
            logger.info(s"External beta deployment for ${runningDeployment.version} can now be submitted for review...")
            AppStoreConnectApi.submitForBetaTesting(appStoreConnectToken, build.buildId)
          case ("external-beta", Some(build@LiveAppBeta(_, _, _, _, "BETA_APPROVED"))) =>
            logger.info(s"External beta deployment for ${runningDeployment.version} can now be distributed to users...")
            AppStoreConnectApi.distributeToExternalTesters(appStoreConnectToken, build.buildId, externalTesterConfig)
          case (_, None) =>
            Try(logger.info(s"Found running deployment ${runningDeployment.version}, but build was not present in App Store Connect response"))
          case _ =>
            Try(logger.info(s"No action was required for ${runningDeployment.version}. Full details are: $attemptToFindBeta"))
        }
      case None =>
        Try(logger.info("No running deployments found."))
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
      // Process one running deployment per lambda execution
      // In practice it's extremely unlikely that there will be two concurrent deployments anyway
      handleBeta <- handleBetaDeployment(env, runningDeployments.headOption, appStoreConnectBetaBuilds, appStoreConnectToken, gitHubConfig)
    } yield {
      handleBeta
    }

    result match {
      case Success(_) => logger.info("Successfully checked/updated deployment status")
      case Failure(exception) => logger.error(s"Failed to check or update deployment status due to: ${exception}", exception)
    }

  }
}
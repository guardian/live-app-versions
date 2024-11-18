package com.gu.config

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import com.gu.AwsIdentity
import com.gu.conf.{ ConfigurationLoader, SSMConfigurationLocation }
import com.eatthepath.pushy.apns.auth.ApnsSigningKey
import software.amazon.awssdk.auth.credentials.{ AwsCredentialsProviderChain => AwsCredentialsProviderChainV2, DefaultCredentialsProvider => DefaultCredentialsProviderV2, ProfileCredentialsProvider => ProfileCredentialsProviderV2 }

object Config {

  def setupAppIdentity(env: Env) = AwsIdentity(
    app = env.app,
    stack = env.stack,
    stage = env.stage,
    region = Aws.euWest1.getName)

  case class Env(app: String, stack: String, stage: String) {
    override def toString: String = s"App: $app, Stack: $stack, Stage: $stage\n"
  }

  object Env {
    def apply(): Env = Env(
      Option(System.getenv("App")).getOrElse("live-app-versions"), // These defaults allow us to use SSM when running locally
      Option(System.getenv("Stack")).getOrElse("mobile"),
      Option(System.getenv("Stage")).getOrElse("CODE"))
  }

  case class AppStoreConnectConfig(teamId: String, keyId: String, issuerId: String, privateKey: ApnsSigningKey, appleAppId: String)

  object AppStoreConnectConfig {

    def apply(env: Env): AppStoreConnectConfig = {

      val ssmPrivateConfig = ConfigurationLoader.load(setupAppIdentity(env), CredentialsProvider.credentialsv2) {
        case identity: AwsIdentity => SSMConfigurationLocation.default(identity)
      }

      val teamId = ssmPrivateConfig.getString("appstore.teamId")
      val keyId = ssmPrivateConfig.getString("appstore.keyId")

      AppStoreConnectConfig(
        teamId = teamId,
        keyId = keyId,
        issuerId = ssmPrivateConfig.getString("appstore.issuerId"),
        privateKey = ApnsSigningKey.loadFromInputStream(
          new ByteArrayInputStream(ssmPrivateConfig.getString("appstore.privateKey").getBytes(StandardCharsets.UTF_8)),
          teamId,
          keyId),
        appleAppId = System.getenv("APPLE_APP_ID"))
    }
  }

  case class GoogleServiceAccount(json: String)

  object GoogleServiceAccount {

    def apply(env: Env): GoogleServiceAccount = {
      val ssmPrivateConfig = ConfigurationLoader.load(setupAppIdentity(env), CredentialsProvider.credentialsv2) {
        case identity: AwsIdentity => SSMConfigurationLocation.default(identity)
      }
      GoogleServiceAccount(ssmPrivateConfig.getString("google.serviceAccountJson"))
    }

  }

  case class ExternalTesterGroup(id: String, name: String)
  case class ExternalTesterConfig(group1: ExternalTesterGroup,
                                  group2: ExternalTesterGroup,
                                  group3: ExternalTesterGroup,
                                  group4: ExternalTesterGroup,
                                  group5: ExternalTesterGroup,
                                  group6: ExternalTesterGroup)
  val externalTesterConfigForProd = ExternalTesterConfig(
    ExternalTesterGroup("b3ee0d21-fe7e-487a-9f81-5ea993b6e860", "External Testers 1"),
    ExternalTesterGroup("53ab9951-d444-4107-87ce-dbfbb2c898e5", "External Testers 2"),
    ExternalTesterGroup("a84bf09f-adf2-403e-a69e-8636cba7cedd", "External Testers 3"),
    ExternalTesterGroup("71e65c76-50b3-412f-8f95-c0195e8716ee", "External Testers 4"),
    ExternalTesterGroup("75de0034-ffe3-475d-bba9-73016808d473", "External Testers 5"),
    ExternalTesterGroup("3f5f1a35-dd71-4e11-8643-b20d0939c071", "Guardian Staff"))
  val externalTesterConfigForTesting = ExternalTesterConfig(
    ExternalTesterGroup("2c761621-6849-46c5-a936-fecc1187d736", "Live App Versions Testers 1"),
    ExternalTesterGroup("d3fc87fc-7416-41ae-8ff9-2a1d8a6c619a", "Live App Versions Testers 2"),
    ExternalTesterGroup("a84bf09f-adf2-403e-a69e-8636cba7cedd", "External Testers 3"),
    ExternalTesterGroup("71e65c76-50b3-412f-8f95-c0195e8716ee", "External Testers 4"),
    ExternalTesterGroup("75de0034-ffe3-475d-bba9-73016808d473", "External Testers 5"),
    ExternalTesterGroup("3f5f1a35-dd71-4e11-8643-b20d0939c071", "Guardian Staff"))

  case class GitHubConfig(token: String)

  object GitHubConfig {
    def apply(env: Env): GitHubConfig = {
      val ssmPrivateConfig = ConfigurationLoader.load(setupAppIdentity(env), CredentialsProvider.credentialsv2) {
        case identity: AwsIdentity => SSMConfigurationLocation.default(identity)
      }
      val token = ssmPrivateConfig.getString("github.token")
      GitHubConfig(token)
    }
  }

  object CredentialsProvider {
    lazy val credentialsv2: AwsCredentialsProviderChainV2 = AwsCredentialsProviderChainV2.of(
      ProfileCredentialsProviderV2.builder.profileName("mobile").build,
      DefaultCredentialsProviderV2.create)
  }

}

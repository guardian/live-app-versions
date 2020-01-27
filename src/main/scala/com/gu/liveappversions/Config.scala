package com.gu.liveappversions

import com.turo.pushy.apns.auth.ApnsSigningKey
import com.gu.AwsIdentity
import com.gu.conf.{ ConfigurationLoader, SSMConfigurationLocation }
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

object Config {

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

      def setupAppIdentity(env: Env) = AwsIdentity(
        app = env.app,
        stack = env.stack,
        stage = env.stage,
        region = Aws.euWest.getName
      )

      val ssmPrivateConfig = ConfigurationLoader.load(setupAppIdentity(env), Aws.credentials) {
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

}

package com.gu.liveappversions

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import com.turo.pushy.apns.auth.ApnsSigningKey

object Config {

  case class Env(app: String, stack: String, stage: String) {
    override def toString: String = s"App: $app, Stack: $stack, Stage: $stage\n"
  }

  object Env {
    def apply(): Env = Env(
      Option(System.getenv("App")).getOrElse("DEV"),
      Option(System.getenv("Stack")).getOrElse("DEV"),
      Option(System.getenv("Stage")).getOrElse("DEV"))
  }

  case class AppStoreConnectConfig(teamId: String, privateKeyId: String, issuerId: String, privateKey: ApnsSigningKey)

  object AppStoreConnectConfig {
    def apply(): AppStoreConnectConfig = {
      val teamId = System.getenv("APPSTORE_TEAM_ID")
      val privateKeyId = System.getenv("APPSTORE_PRIVATE_KEY_ID")
      AppStoreConnectConfig(
        teamId = teamId,
        privateKeyId = privateKeyId,
        issuerId = System.getenv("APPSTORE_ISSUER_ID"),
        privateKey = ApnsSigningKey.loadFromInputStream(
          new ByteArrayInputStream(System.getenv("APPSTORE_PRIVATE_KEY").getBytes(StandardCharsets.UTF_8)),
          teamId,
          privateKeyId))
    }
  }

}

package com.gu.config

import com.eatthepath.pushy.apns.auth.ApnsSigningKey
import com.gu.AwsIdentity
import com.gu.conf.{ ConfigurationLoader, SSMConfigurationLocation }
import io.jsonwebtoken.Jwts
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.{ PEMKeyPair, PEMParser }
import org.kohsuke.github.GHPermissionType.WRITE
import org.kohsuke.github.GitHubBuilder
import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProviderChain => AwsCredentialsProviderChainV2,
  DefaultCredentialsProvider => DefaultCredentialsProviderV2,
  ProfileCredentialsProvider => ProfileCredentialsProviderV2
}

import java.io.{ ByteArrayInputStream, StringReader }
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.time.Clock
import java.time.Clock.systemUTC
import java.time.Duration.ofMinutes
import java.util.Date
import scala.jdk.CollectionConverters._
import scala.util.Using

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
  case class ExternalTesterConfig(
    group1: ExternalTesterGroup,
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

  /**
   * Generates a GitHub App installation token, which can be used just like a PAT to get access to GitHub resources.
   * Its advantage is that it is short-lived and not tied to any user.
   *
   * Generating an installation token has three steps:
   *  1. Use the app's client ID and private key to generate a JWT.
   *  2. Look up the installation ID (assuming the app is only installed in one org).
   *  3. Use the JWT and installation ID to generate a token.
   *
   * See
   * [[https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/authenticating-as-a-github-app-installation]]
   */
  object GitHubConfig {
    def apply(env: Env): GitHubConfig = {
      val ssmPrivateConfig = ConfigurationLoader.load(setupAppIdentity(env), CredentialsProvider.credentialsv2) {
        case identity: AwsIdentity => SSMConfigurationLocation.default(identity)
      }

      // Read GitHub App private key from a PEM string.
      def readPrivateKey(pem: String): PrivateKey =
        Using.resource(new StringReader(pem)) { reader =>
          val pemParser = new PEMParser(reader)
          val pemObject = pemParser.readObject()
          val keyPair = pemObject match {
            case kp: PEMKeyPair => kp
            case _ => throw new IllegalArgumentException("Invalid PEM format")
          }
          val converter = new JcaPEMKeyConverter()
          converter.getPrivateKey(keyPair.getPrivateKeyInfo)
        }

      // See https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-a-json-web-token-jwt-for-a-github-app
      def generateJwt(appClientId: String, privateKey: PrivateKey)(implicit clock: Clock = systemUTC()): String = {
        val now = clock.instant()
        Jwts
          .builder()
          .issuer(appClientId)
          // To protect against clock drift
          .issuedAt(Date.from(now.minusSeconds(60)))
          // Max TTL GitHub will allow
          .expiration(Date.from(now.plus(ofMinutes(10))))
          .signWith(privateKey)
          .compact()
      }

      val appClientId = ssmPrivateConfig.getString("github.app.clientId")
      val appPrivateKeyString = ssmPrivateConfig.getString("github.app.privateKey")

      val appPrivateKey = readPrivateKey(appPrivateKeyString)
      val jwt = generateJwt(appClientId, appPrivateKey)
      val github = new GitHubBuilder().withJwtToken(jwt).build()

      // Get first, ie only, installation ID for the GitHub app
      val installationId = github.getApp.listInstallations().toList.asScala.headOption
        .getOrElse(throw new NoSuchElementException("No installations found for the GitHub app"))
        .getId

      val token = github.getApp
        .getInstallationById(installationId)
        .createToken()
        .repositories(List("ios-live").asJava)
        .permissions(Map("deployments" -> WRITE).asJava)
        .create()
        .getToken

      GitHubConfig(token)
    }
  }

  object CredentialsProvider {
    lazy val credentialsv2: AwsCredentialsProviderChainV2 = AwsCredentialsProviderChainV2.of(
      ProfileCredentialsProviderV2.builder.profileName("mobile").build,
      DefaultCredentialsProviderV2.create)
  }

}

package com.gu.appstoreconnectapi

import java.time.ZonedDateTime

import com.gu.config.Config.AppStoreConnectConfig
import pdi.jwt.{ Jwt, JwtAlgorithm, JwtClaim, JwtHeader }

object JwtTokenBuilder {

  def buildToken(appStoreConnectConfig: AppStoreConnectConfig): String = {
    val expires = ZonedDateTime.now.plusMinutes(20).toInstant.getEpochSecond
    val header = new JwtHeader(
      algorithm = Some(JwtAlgorithm.ES256),
      typ = Some("typ"),
      keyId = Some(appStoreConnectConfig.keyId),
      contentType = Some("application/json"))
    val claim = JwtClaim(
      issuer = Some(appStoreConnectConfig.issuerId),
      expiration = Some(expires),
      audience = Some(Set("appstoreconnect-v1")))
    Jwt.encode(header = header, claim = claim, key = appStoreConnectConfig.privateKey)
  }

}
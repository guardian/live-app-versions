package com.gu.playdeveloperapi

import java.io.ByteArrayInputStream

import com.google.auth.oauth2.{ AccessToken, GoogleCredentials }
import com.gu.config.Config.{ Env, GoogleServiceAccount }

import scala.util.Try

object Token {

  def getToken(env: Env): Try[AccessToken] = Try {
    val credentials = GoogleCredentials
      .fromStream(new ByteArrayInputStream(GoogleServiceAccount(env).json.getBytes))
      .createScoped("https://www.googleapis.com/auth/androidpublisher")
    credentials.refresh()
    credentials.getAccessToken
  }

}


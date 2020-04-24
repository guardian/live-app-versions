package com.gu.config

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{ AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider }
import com.amazonaws.regions.Regions

object Aws {

  val euWest1 = Regions.EU_WEST_1

  def credentials(profileName: String) = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider(profileName), // Used when running locally
    new EnvironmentVariableCredentialsProvider() // Used by AWS lambda
  )

}

package com.gu.config

import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.auth.credentials.{ EnvironmentVariableCredentialsProvider, ProfileCredentialsProvider }
import software.amazon.awssdk.regions.Region

object Aws {

  val euWest1 = Region.EU_WEST_1

  def credentials(profileName: String) = AwsCredentialsProviderChain.of(
    ProfileCredentialsProvider.create(profileName), // Used when running locally
    EnvironmentVariableCredentialsProvider.create() // Used by AWS lambda
  )

}

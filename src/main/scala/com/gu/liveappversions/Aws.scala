package com.gu.liveappversions

import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions

object Aws {

  val euWest = Regions.EU_WEST_1

  val credentials = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("mobile"), // Used when running locally
    new EnvironmentVariableCredentialsProvider() // Used by AWS lambda
  )

}

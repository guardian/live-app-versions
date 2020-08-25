package com.gu.liveappversions.android

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

case class BuildInfo(seenInBeta: Boolean)

object BuildInfo {

  implicit val buildOutputEncoder: Encoder[BuildInfo] = deriveEncoder[BuildInfo]

}

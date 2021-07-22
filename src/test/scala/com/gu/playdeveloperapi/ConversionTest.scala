package com.gu.playdeveloperapi

import com.gu.playdeveloperapi.Conversion.Version
import com.gu.playdeveloperapi.PlayDeveloperApi.PlayDeveloperApi.{ Release, Track }
import org.scalatest.FunSuite

class ConversionTest extends FunSuite {

  val betaTrack = Track("beta", releases = List(Release("1.2.123", "completed")))
  val productionTrack = Track("production", releases = List(Release("1.2.123", "completed")))
  val productionTrackWithDraft = Track("production", releases = List(Release("1.2.123", "draft")))

  test("searchForTrack correctly identifies a beta build") {
    val result = Conversion.searchForTrack(List(betaTrack, productionTrack), "beta")
    assert(result == Some(Version("1.2.123")))
  }

  test("searchForTrack correctly ignores a draft production build") {
    val result = Conversion.searchForTrack(List(betaTrack, productionTrackWithDraft), "production")
    assert(result == None)
  }

}

package com.gu.playdeveloperapi

import com.google.auth.oauth2.AccessToken
import com.gu.okhttp.SharedClient
import com.gu.playdeveloperapi.Conversion.AndroidLiveAppVersions
import io.circe.generic.auto._
import io.circe.parser.decode
import okhttp3.{Request, RequestBody}
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.Try

object PlayDeveloperApi {

  object PlayDeveloperApi {

    val logger: Logger = LoggerFactory.getLogger(this.getClass)

    val baseUrl = "https://www.googleapis.com/androidpublisher/v3/applications/com.guardian/edits"

    private def createEdit(accessToken: AccessToken): Try[EditId] = {

      val request = new Request.Builder()
        .url(baseUrl)
        .addHeader("Authorization", s"Bearer ${accessToken.getTokenValue}")
        .post(RequestBody.create("", null))
        .build()

      for {
        httpResponse <- Try(SharedClient.client.newCall(request).execute)
        bodyAsString <- SharedClient.getResponseBodyIfSuccessful("Google Play Developer API Response: (edits.insert) ", httpResponse)
        editId <- decode[EditId](bodyAsString).toTry
      } yield {
        logger.info(s"The response for edit ID: $bodyAsString")
        logger.info(s"The edit ID is: $editId")
        editId
      }

    }

    private def listTrackInfo(accessToken: AccessToken, openEdit: EditId): Try[TracksResponse] = {

      val request = new Request.Builder()
        .url(s"$baseUrl/${openEdit.id}/tracks")
        .addHeader("Authorization", s"Bearer ${accessToken.getTokenValue}")
        .get()
        .build()

      for {
        httpResponse <- Try(SharedClient.client.newCall(request).execute)
        bodyAsString <- SharedClient.getResponseBodyIfSuccessful("Google Play Developer API Response: (edits.tracks.list) ", httpResponse)
        tracksResponse <- decode[TracksResponse](bodyAsString).toTry
      } yield {
        logger.info(s"The response for tracks: $bodyAsString")
        logger.info(s"The track and release is: $tracksResponse")
        tracksResponse
      }

    }

    def getBetaAndProductionVersions(accessToken: AccessToken): Try[AndroidLiveAppVersions] = {
      for {
        edit <- createEdit(accessToken)
        trackResponse <- listTrackInfo(accessToken, edit)
        versions <- Conversion.toAndroidLiveAppVersions(trackResponse)
      } yield {
        versions
      }
    }

    case class EditId(id: String)

    case class Release(name: String, status: String)
    case class Track(track: String, releases: List[Release])
    case class TracksResponse(tracks: List[Track])

  }

}

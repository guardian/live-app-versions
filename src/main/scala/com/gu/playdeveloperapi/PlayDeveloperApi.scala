package com.gu.playdeveloperapi

import com.google.auth.oauth2.AccessToken
import com.gu.okhttp.SharedClient
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.parser._
import okhttp3.{ Request, RequestBody }

import scala.util.Try

object PlayDeveloperApi {

  object PlayDeveloperApi {

    val baseUrl = "https://www.googleapis.com/androidpublisher/v3/applications/com.guardian/edits"

    private def createEdit(accessToken: AccessToken): Try[EditId] = {

      val request = new Request.Builder()
        .url(baseUrl)
        .addHeader("Authorization", s"Bearer ${accessToken.getTokenValue}")
        .post(RequestBody.create("", null))
        .build()

      for {
        httpResponse <- Try(SharedClient.client.newCall(request).execute)
        bodyAsString <- SharedClient.getResponseBodyIfSuccessful("Google Play Developer API", httpResponse)
        editId <- decode[EditId](bodyAsString).toTry
      } yield editId

    }

    private def listTrackInfo(accessToken: AccessToken, openEdit: EditId): Try[TracksResponse] = {

      val request = new Request.Builder()
        .url(s"$baseUrl/${openEdit.id}/tracks")
        .addHeader("Authorization", s"Bearer ${accessToken.getTokenValue}")
        .get()
        .build()

      for {
        httpResponse <- Try(SharedClient.client.newCall(request).execute)
        bodyAsString <- SharedClient.getResponseBodyIfSuccessful("Google Play Developer API", httpResponse)
        tracksResponse <- decode[TracksResponse](bodyAsString).toTry
      } yield tracksResponse

    }

    def getTrackInfo(accessToken: AccessToken): Try[String] = {
      for {
        edit <- createEdit(accessToken)
        trackResponse <- listTrackInfo(accessToken, edit)
      } yield {
        trackResponse.tracks.find(_.track == "production").toString
      }
    }

    case class EditId(id: String)

    case class Release(name: String)
    case class Track(track: String, releases: List[Release])
    case class TracksResponse(tracks: List[Track])

  }

}

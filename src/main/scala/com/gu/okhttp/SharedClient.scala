package com.gu.okhttp

import okhttp3.{ OkHttpClient, Response }
import scala.util.{ Failure, Success, Try }

case class ApiException(message: String) extends Throwable(message: String)

object SharedClient {

  val client = new OkHttpClient

  def getResponseBodyIfSuccessful(apiName: String, response: Response): Try[String] = {
    val responseBody = response.body().string()
    response.body().close() //https://square.github.io/okhttp/4.x/okhttp/okhttp3/-response-body/#the-response-body-must-be-closed
    if (!response.isSuccessful) {
      Failure(
        ApiException(
          s"Received an unsuccessful response from $apiName. Response code: ${response.code()} | response body: ${responseBody}"))
    } else {
      Success(responseBody)
    }
  }

}

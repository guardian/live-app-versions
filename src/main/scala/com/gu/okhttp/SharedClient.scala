package com.gu.okhttp

import okhttp3.{OkHttpClient, Response}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

case class ApiException(message: String) extends Throwable(message: String)

object SharedClient {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val client = new OkHttpClient

  def getResponseBodyIfSuccessful(apiName: String, response: Response): Try[String] = {
    val responseBody = response.body().string()
    response.body().close() //https://square.github.io/okhttp/4.x/okhttp/okhttp3/-response-body/#the-response-body-must-be-closed
    if (!response.isSuccessful) {
      val message = s"Received an unsuccessful response from $apiName. Response code: ${response.code()} | response body: ${responseBody}"
      logger.warn(message)
      Failure(ApiException(message))
    } else {
      logger.info(s"Received successful response from $apiName. Response code: ${response.code()} | response body: ${responseBody}")
      Success(responseBody)
    }
  }

}

package com.gu.liveappversions

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{ ObjectCannedACL, GetObjectRequest, PutObjectRequest, PutObjectResponse, S3Exception }
import software.amazon.awssdk.core.sync.RequestBody
import com.gu.config.Aws
import com.gu.config.Config.Env
import com.gu.liveappversions.ios.Lambda.logger
import io.circe.Json
import io.circe.syntax._

import scala.io.Source
import scala.util.{ Failure, Success, Try }

object S3Storage {

  private val s3Client = S3Client
    .builder()
    .credentialsProvider(Aws.credentials("developerPlayground"))
    .region(Aws.euWest1)
    .build()

  def fullKey(env: Env, partialKey: String): String = {
    val stagePrefix = if (env.stage == "PROD") "/" else s"/${env.stage}/"
    s"reserved-paths$stagePrefix$partialKey"
  }

  def putJson(jsonToUpload: Json, env: Env, bucketName: String, partialKey: String): Try[PutObjectResponse] = {
    val key = fullKey(env, partialKey)
    val accessControl = if (env.stage == "PROD") ObjectCannedACL.PUBLIC_READ else ObjectCannedACL.PRIVATE
    val bytes = jsonToUpload.toString().getBytes(StandardCharsets.UTF_8)
    val stream = new ByteArrayInputStream(bytes)

    val putRequest = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .contentType("application/json")
      .cacheControl("max-age=60")
      .acl(accessControl)
      .build()

    Try {
      val requestBody = RequestBody.fromInputStream(stream, bytes.length)
      s3Client.putObject(putRequest, requestBody)
    } match {
      case Success(result) =>
        logger.info(s"Successfully uploaded new build info to S3 (bucket: $bucketName | key: $key)")
        stream.close()
        Success(result)
      case Failure(exception) =>
        logger.error(s"Failed to uploaded build to S3 (bucket: $bucketName | key: $key) due to: $exception")
        stream.close()
        Failure(exception)
    }
  }

  def getJsonString(env: Env, bucketName: String, partialKey: String): Try[Option[String]] = {
    val key = fullKey(env, partialKey)

    val getRequest = GetObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .build()

    Try {
      val inputStream = s3Client.getObject(getRequest)
      try {
        Source.fromInputStream(inputStream).mkString
      } finally {
        inputStream.close()
      }
    } match {
      case Success(result) =>
        logger.info(s"Successfully downloaded file from S3 (bucket: $bucketName | key: $key)")
        Success(Some(result))
      case Failure(s3Exception: S3Exception) if (s3Exception.statusCode() == 404) =>
        logger.info(s"File was not present in s3 (bucket: $bucketName | key: $key)")
        Success(None)
      case Failure(exception) =>
        logger.error(s"Failed to download file from S3 (bucket: $bucketName | key: $key) due to: $exception")
        Failure(exception)
    }
  }
}

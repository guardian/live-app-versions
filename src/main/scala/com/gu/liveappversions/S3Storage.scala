package com.gu.liveappversions

import java.nio.charset.StandardCharsets

import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{ GetObjectRequest, GetObjectResponse, PutObjectRequest, PutObjectResponse, S3Exception }
import com.gu.config.Aws
import com.gu.config.Config.Env
import com.gu.liveappversions.ios.Lambda.logger
import io.circe.Json

import scala.io.Source
import scala.util.{ Failure, Success, Try, Using }

object S3Storage {

  private lazy val s3Client = S3Client
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
    val bytes = jsonToUpload.toString().getBytes(StandardCharsets.UTF_8)

    val putRequest = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .contentType("application/json")
      .cacheControl("max-age=60")
      .build()

    val requestBody = RequestBody.fromBytes(bytes)

    Try(s3Client.putObject(putRequest, requestBody)) match {
      case Success(result) =>
        logger.info(s"Successfully uploaded new build info to S3 (bucket: $bucketName | key: $key)")
        Success(result)
      case Failure(exception) =>
        logger.error(s"Failed to upload build to S3 (bucket: $bucketName | key: $key)", exception)
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
      Using.resource(s3Client.getObject(getRequest)) { (responseInputStream: ResponseInputStream[GetObjectResponse]) =>
        Using.resource(Source.fromInputStream(responseInputStream)) { source =>
          source.mkString
        }
      }
    } match {
      case Success(result) =>
        logger.info(s"Successfully downloaded file from S3 (bucket: $bucketName | key: $key)")
        Success(Some(result))
      case Failure(s3Exception: S3Exception) if (s3Exception.statusCode() == 404) =>
        logger.info(s"File was not present in s3 (bucket: $bucketName | key: $key)")
        Success(None)
      case Failure(exception) =>
        logger.error(s"Failed to download file from S3 (bucket: $bucketName | key: $key)", exception)
        Failure(exception)
    }
  }
}

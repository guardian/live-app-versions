package com.gu.liveappversions

import java.io.ByteArrayInputStream

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{ CannedAccessControlList, ObjectMetadata, PutObjectRequest, PutObjectResult }
import com.gu.config.Aws
import com.gu.config.Config.Env
import com.gu.liveappversions.ios.Lambda.logger
import io.circe.Json
import io.circe.syntax._

import scala.io.Source
import scala.util.{ Failure, Success, Try }

object S3Storage {

  private val s3Client = AmazonS3ClientBuilder.standard()
    .withCredentials(Aws.credentials("developerPlayground"))
    .withRegion(Aws.euWest1.getName)
    .build()

  def fullKey(env: Env, partialKey: String): String = {
    val stagePrefix = if (env.stage == "PROD") "/" else s"/${env.stage}/"
    s"reserved-paths$stagePrefix$partialKey"
  }

  def putJson(jsonToUpload: Json, env: Env, bucketName: String, partialKey: String): Try[PutObjectResult] = {

    val key = fullKey(env, partialKey)

    val buildAttributesStream: ByteArrayInputStream = new ByteArrayInputStream(jsonToUpload.toString().getBytes)

    val metadata = new ObjectMetadata()
    metadata.setContentType("application/json")
    metadata.setCacheControl("max-age=60")

    val accessControl = if (env.stage == "PROD" || env.stage == "CODE") {
      CannedAccessControlList.PublicRead
    } else {
      CannedAccessControlList.Private //It's preferable to avoid serving test files via https://mobile.guardianapis.com/
    }

    val putObjectRequest = new PutObjectRequest(bucketName, key, buildAttributesStream, metadata)
      .withCannedAcl(accessControl)

    Try(s3Client.putObject(putObjectRequest)) match {
      case Success(result) =>
        logger.info(s"Successfully uploaded new build info to S3 (bucket: $bucketName | key: $key)")
        Success(result)
      case Failure(exception) =>
        logger.error(s"Failed to uploaded build to S3 (bucket: $bucketName | key: $key) due to: $exception")
        Failure(exception)
    }

  }

  def getJsonString(env: Env, bucketName: String, partialKey: String): Try[Option[String]] = {
    val key = fullKey(env, partialKey)
    Try {
      val s3Object = s3Client.getObject(bucketName, key)
      Source.fromInputStream(s3Object.getObjectContent).mkString
    } match {
      case Success(result) =>
        logger.info(s"Successfully downloaded file from S3 (bucket: $bucketName | key: $key)")
        Success(Some(result))
      case Failure(amazonServiceException: AmazonServiceException) if (amazonServiceException.getStatusCode == 404) =>
        logger.info(s"File was not present in s3 (bucket: $bucketName | key: $key)")
        Success(None)
      case Failure(exception) =>
        logger.error(s"Failed to download file from S3 (bucket: $bucketName | key: $key) due to: $exception")
        Failure(exception)
    }
  }

}

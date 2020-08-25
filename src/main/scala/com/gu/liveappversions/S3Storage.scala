package com.gu.liveappversions

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{ CannedAccessControlList, ObjectMetadata, PutObjectRequest, PutObjectResult }
import com.gu.config.Aws
import com.gu.config.Config.Env
import com.gu.liveappversions.ios.Lambda.logger
import io.circe.Json

import scala.util.{ Failure, Success, Try }

object S3Storage {

  private val s3Client = AmazonS3ClientBuilder.standard()
    .withCredentials(Aws.credentials("developerPlayground"))
    .withRegion(Aws.euWest1.getName)
    .build()

  def storageLocation(env: Env, partialKey: String): String = {
    val stagePrefix = if (env.stage == "PROD") "/" else s"/${env.stage}/"
    s"reserved-paths$stagePrefix$partialKey"
  }

  def attemptUpload(jsonToUpload: Json, env: Env, bucketName: String, partialKey: String): Try[PutObjectResult] = {

    val key = storageLocation(env, partialKey)

    val buildAttributesStream: ByteArrayInputStream = new ByteArrayInputStream(jsonToUpload.toString().getBytes)

    val metadata = new ObjectMetadata()
    metadata.setContentType("application/json")
    metadata.setCacheControl("max-age=60")

    val accessControl = if (env.stage == "PROD") {
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

}

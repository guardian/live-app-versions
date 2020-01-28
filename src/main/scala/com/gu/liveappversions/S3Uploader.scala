package com.gu.liveappversions

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{ CannedAccessControlList, ObjectMetadata, PutObjectRequest }
import com.gu.liveappversions.Config.Env
import com.gu.liveappversions.Lambda.logger
import io.circe.syntax._

import scala.util.{ Failure, Success, Try }

object S3Uploader {

  private val s3Client = AmazonS3ClientBuilder.standard()
    .withCredentials(Aws.credentials("developerPlayground"))
    .withRegion(Aws.euWest1.getName)
    .build()

  def attemptUpload(buildOutput: BuildOutput, env: Env, bucketName: String): Unit = {

    val stagePrefix = if (env.stage == "PROD") { "/" } else { s"/${env.stage}/" }
    val fileObjectKeyName = s"reserved-paths${stagePrefix}ios-live-app/recent-beta-releases.json"
    val buildAttributesStream: ByteArrayInputStream = new ByteArrayInputStream(buildOutput.asJson.toString().getBytes)

    val metadata = new ObjectMetadata()
    metadata.setContentType("application/json")
    metadata.setCacheControl("max-age=60")

    val accessControl = if (env.stage == "PROD") {
      CannedAccessControlList.PublicRead
    } else {
      CannedAccessControlList.Private //It's preferable to avoid serving test files via https://mobile.guardianapis.com/
    }

    val putObjectRequest = new PutObjectRequest(bucketName, fileObjectKeyName, buildAttributesStream, metadata)
      .withCannedAcl(accessControl)

    Try(s3Client.putObject(putObjectRequest)) match {
      case Success(_) =>
        logger.info(s"Successfully uploaded new build info to S3 (bucket: $bucketName | keyName: $fileObjectKeyName)")
      case Failure(exception) =>
        logger.error(s"Failed to uploaded build to S3 (bucket: $bucketName | keyName: $fileObjectKeyName) due to: $exception")
        throw exception
    }

  }

}

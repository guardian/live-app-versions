package com.gu.liveappversions

import com.amazonaws.services.s3.AmazonS3ClientBuilder

object S3Uploader {

  val s3Client = AmazonS3ClientBuilder.standard()
    .withRegion(Aws.euWest.getName)
    .build();

}

package com.gu.liveappversions

import com.amazonaws.services.s3.model.PutObjectResult
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Success, Try }

object UploadAttempt {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def handle(attempt: Try[PutObjectResult]) = {
    attempt match {
      case Success(_) =>
        logger.info("Successfully updated build information")
      case Failure(exception) =>
        logger.error(s"Failed to update build information due to $exception")
        throw exception // This allows us to monitor failures easily (using standard AWS metrics)
    }
  }

}

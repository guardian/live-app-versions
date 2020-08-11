package com.gu.liveappversions.android

import com.gu.config.Config.Env
import org.slf4j.{ Logger, LoggerFactory }

object Lambda {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def handler(): Unit = {
    val env = Env()
    logger.info(s"Starting $env")
  }

}

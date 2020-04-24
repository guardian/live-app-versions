package com.gu.iosdeployments

import com.amazonaws.services.lambda.runtime.Context
import com.gu.config.Config.Env
import org.slf4j.{ Logger, LoggerFactory }

object Lambda {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def handler(context: Context): Unit = {
    val env = Env()
    logger.info(s"Starting $env")
    process(env)
  }

  def process(env: Env): Unit = {
    logger.info("Hello world")
  }

}

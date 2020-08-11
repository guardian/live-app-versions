package com.gu.liveappversions.android

import com.gu.config.Config.Env
import com.gu.playdeveloperapi.PlayDeveloperApi.PlayDeveloperApi
import com.gu.playdeveloperapi.Token
import org.slf4j.{ Logger, LoggerFactory }

object Lambda {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def handler(): Unit = {
    val env = Env()
    logger.info(s"Starting $env")
    for {
      token <- Token.getToken(env)
      tracks <- PlayDeveloperApi.getTrackInfo(token)
    } yield {
      logger.info(s"Retrieved track info: $tracks")
    }

  }

}

object Test extends App {
  Lambda.handler()
}

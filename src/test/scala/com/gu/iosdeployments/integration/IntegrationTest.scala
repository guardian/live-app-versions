package com.gu.iosdeployments.integration

import com.gu.config.Config.Env
import com.gu.iosdeployments.Lambda

object IntegrationTest {

  def main(args: Array[String]): Unit = {
    Lambda.process(Env())
  }

}

package com.gu.liveappversions.integration

import com.gu.config.Config.Env
import com.gu.liveappversions.Lambda

object IntegrationTest {

  def main(args: Array[String]): Unit = {
    Lambda.process(Env(), "jacob-w-test") // This bucket lives in the Developer Playground account (to avoid polluting Mobile)
  }

}

package com.gu.liveappversions.integration

import com.gu.liveappversions.Config.Env
import com.gu.liveappversions.Lambda

object IntegrationTest {

  def main(args: Array[String]): Unit = {
    Lambda.process(Env())
  }

}

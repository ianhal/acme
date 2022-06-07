package com.acme
package utils

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{FiniteDuration, SECONDS, TimeUnit}

object Rich {
  implicit val defaultBuildUnits: TimeUnit = SECONDS

  implicit class RichInt(private val self: Int) {
    def toFiniteDuration(implicit timeUnit: TimeUnit): FiniteDuration = FiniteDuration.apply(self, timeUnit)
  }

}

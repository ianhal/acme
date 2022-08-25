package com.acme
package utils

import cats.effect.IO
import org.slf4j.Marker

trait LogSupportIO { self =>
  import com.typesafe.scalalogging.{Logger => ScalaLogger}

  protected val logger: ScalaLogger = ScalaLogger.apply(getClass)
  implicit val logger_ : ScalaLogger = logger


  def debugIO[A](message: => A): IO[Unit] = IO { logger.debug(message.toString) }
  def debugIO(message: => String, throwable: Throwable): IO[Unit] = IO { logger.debug(message, throwable) }
  def debugIO(marker: Marker, message: => String, throwable: Throwable): IO[Unit] = IO { logger.debug(marker, message, throwable) }

  def info(message: => String): Unit = logger.info(message)
  def infoIO[A](message: => A): IO[Unit] = IO { logger.info(message.toString) }
  def infoIO(message: => String, throwable: Throwable): IO[Unit] = IO { logger.info(message, throwable) }
  def infoIO(marker: Marker, message: => String, throwable: Throwable): IO[Unit] = IO { logger.info(marker, message, throwable) }

  def warn(message: => String): Unit = logger.warn(message)
  def warnIO(message: => String): IO[Unit] = IO { logger.warn(message) }
  def warnIO(message: => String, throwable: Throwable): IO[Unit] = IO { logger.warn(message, throwable) }
  def warnIO(marker: Marker, message: => String, throwable: Throwable): IO[Unit] = IO { logger.warn(marker, message, throwable) }

  def error(message: => String): Unit = logger.error(message)
  def errorIO(message: => String): IO[Unit] = IO { logger.error(message) }
  def errorIO(message: => String, throwable: Throwable): IO[Unit] = IO { logger.error(message, throwable) }
  def errorIO(marker: Marker, message: => String, throwable: Throwable): IO[Unit] = IO { logger.error(marker, message, throwable) }
}

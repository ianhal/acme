package com.acme
package utils

import cats.effect.{IO, Sync}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{FiniteDuration, SECONDS, TimeUnit}
import scala.util.Random

object Rich {
  implicit val defaultBuildUnits: TimeUnit = SECONDS

  implicit class RichInt(private val self: Int) {
    def toFiniteDuration(implicit timeUnit: TimeUnit): FiniteDuration = FiniteDuration.apply(self, timeUnit)
  }

  implicit class RichList[A](private val list: List[A]) extends AnyVal {

    private def randomIdx: Int = Random.nextInt(list.length)

    def maybeRandom: Option[A] = if(list.nonEmpty) Some(list(randomIdx)) else None

  }

  implicit class RichIO[A](private val io: IO[A]) extends AnyVal {

    def loopForever: IO[Nothing] = io.flatMap(_ => loopForever)

    def notFasterThan(time: FiniteDuration): IO[A] = {
      for {
        waitingForTimePassed <- IO.sleep(time).start
        a <- io
        _ <- waitingForTimePassed.join
      } yield a
    }
  }

  implicit class RichIO_Option[A](private val io: IO[Option[A]]) extends AnyVal {
    def loopUntilSome: IO[A] = IO.suspend(Sync.Type.Delay)(io.flatMap(f => f.fold(loopUntilSome)(IO.pure))).flatten
  }

}

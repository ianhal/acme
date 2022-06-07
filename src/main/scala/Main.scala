package com.acme

import config.AcmeConfig
import utils.LogSupportIO

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp with LogSupportIO {

  override def run(args: List[String]): IO[ExitCode] = for {
    config <- AcmeConfig.createIO
    _ <- debugIO(config)
  } yield ExitCode.Success
}

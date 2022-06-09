package com.acme

import config.AcmeConfig
import consumer.AssemblerRobot
import domain.Component
import factory.{Factory, PeekableDequeue}
import producer.Supplier
import utils.LogSupportIO

import cats.effect.std.Semaphore
import cats.effect.{ExitCode, IO, IOApp, Ref}

import java.util.Calendar

object Main extends IOApp with LogSupportIO {

  import utils.Rich.RichIO

  override def run(args: List[String]): IO[ExitCode] = for {
    config <- AcmeConfig.createIO
    lastTimeRef <- Ref[IO].of(Calendar.getInstance())
    semaphore <- Semaphore[IO](1)
    dequeue <- PeekableDequeue[IO].create[Component](config.factoryConfig, lastTimeRef)
    supplier <- Supplier.create(config.supplierConfig)
    wetRobotBuilder <- AssemblerRobot.createWetRobotIO(dequeue, semaphore, config.consumerConfig)
    dryRobotBuilder <- AssemblerRobot.createDryRobotIO(dequeue, semaphore, config.consumerConfig)
    _ <- Factory.startIO(semaphore, dequeue, lastTimeRef)(supplier, Seq(wetRobotBuilder, dryRobotBuilder), config.factoryConfig)
    _ <- IO.unit.loopForever
  } yield ExitCode.Success
}

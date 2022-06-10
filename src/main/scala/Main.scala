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
    lastTakeRef <- Ref[IO].of(Calendar.getInstance())
    conveyorSemaphore <- Semaphore[IO](1)
    dequeue <- PeekableDequeue[IO].create[Component](config.factoryConfig, lastTakeRef)
    supplier <- Supplier.createIO(config.supplierConfig)
    wetRobotBuilder <- AssemblerRobot.createWetRobotIO(dequeue, conveyorSemaphore, config.consumerConfig)
    dryRobotBuilder <- AssemblerRobot.createDryRobotIO(dequeue, conveyorSemaphore, config.consumerConfig)
    factory <- Factory.factoryIO(conveyorSemaphore, dequeue, lastTakeRef, supplier, Seq(wetRobotBuilder, dryRobotBuilder), config.factoryConfig)
    _ <- factory.startIO
    _ <- IO.unit.loopForever
  } yield ExitCode.Success
}

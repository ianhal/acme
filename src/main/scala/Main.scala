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
    dequeue <- PeekableDequeue.create[Component](
      lastTakeRef = lastTakeRef,
      config = config.factoryConfig
    )
    supplier <- Supplier.createIO(
      lastTakeRef = lastTakeRef,
      dequeue = dequeue,
      conveyorSemaphore = conveyorSemaphore,
      supplierConfig = config.supplierConfig
    )
    wetRobotBuilder <- AssemblerRobot.createWetRobotIO(
      dequeue = dequeue,
      conveyorSemaphore = conveyorSemaphore,
      consumerConfig = config.consumerConfig
    )
    dryRobotBuilder <- AssemblerRobot.createDryRobotIO(
      dequeue = dequeue,
      conveyorSemaphore = conveyorSemaphore,
      consumerConfig = config.consumerConfig
    )
    factory <- Factory.factoryIO(
      supplier = supplier,
      consumers = Seq(wetRobotBuilder, dryRobotBuilder),
      factoryConfig = config.factoryConfig
    )
    _ <- factory.startIO
    _ <- IO.unit.loopForever
  } yield ExitCode.Success
}

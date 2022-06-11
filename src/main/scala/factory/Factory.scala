package com.acme
package factory

import config.AcmeConfig.FactoryConfig
import consumer.AssemblerRobot
import domain.Component
import producer.Supplier
import utils.LogSupportIO

import cats.effect.IO
import cats.effect.std.Semaphore

import scala.concurrent.duration.DurationInt

case class Factory(conveyorSemaphore: Semaphore[IO],
                   dequeue: PeekableDequeue[IO, Component],
                   supplier: Supplier,
                   consumers: Seq[AssemblerRobot],
                   factoryConfig: FactoryConfig
                  ) extends LogSupportIO {

  import utils.Rich.RichIO

  import cats.syntax.parallel._

  def startIO: IO[Unit] = for {
    _ <- infoIO("Starting Factory")
    _ <- supplierAddToQueue.loopForever.start.map(_.cancel)
    _ <- consumers.map(_.startIO()).parSequence
    - <- supplier.checkForStaleComponentIO(dequeue).notFasterThan(2.seconds).loopForever.start.map(_.cancel)
  } yield ()

  def supplierAddToQueue: IO[Unit] = for {
    _ <- debugIO("acquiring semaphore") *> conveyorSemaphore.acquire *> debugIO("acquired semaphore")
    component <- supplier.supplyComponentIO
    _ <- dequeue.put(component)
    _ <- debugIO("releasing semaphore") *> conveyorSemaphore.release *> debugIO("released semaphore")
  } yield ()

}
object Factory{
  def factoryIO(conveyorSemaphore: Semaphore[IO],
                queue: PeekableDequeue[IO, Component],
                producer: Supplier,
                consumers: Seq[AssemblerRobot],
                factoryConfig: FactoryConfig
             ): IO[Factory] = IO(Factory(conveyorSemaphore, queue, producer, consumers, factoryConfig))
}

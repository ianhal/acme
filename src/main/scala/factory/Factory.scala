package com.acme
package factory

import config.AcmeConfig.FactoryConfig
import consumer.AssemblerRobot
import domain.Component
import producer.Supplier
import utils.LogSupportIO

import cats.Monad
import cats.effect.std.Semaphore
import cats.effect.{IO, Ref}

import java.util.Calendar
import scala.concurrent.duration.DurationInt

case class Factory(conveyorSemaphore: Semaphore[IO],
                   dequeue: PeekableDequeue[IO, Component],
                   lastTakeRef: Ref[IO, Calendar], supplier: Supplier,
                   consumers: Seq[AssemblerRobot],
                   factoryConfig: FactoryConfig
                  ) extends LogSupportIO {

  import utils.Rich.RichIO

  import cats.syntax.parallel._

  def startIO: IO[Unit] = for {
    _ <- infoIO("Starting Factory")
    _ <- supplierAddToQueue.loopForever.start.map(_.cancel)
    _ <- consumers.map(_.startIO()).parSequence
    - <- checkForStaleComponent(dequeue).notFasterThan(2.seconds).loopForever.start.map(_.cancel)
  } yield ()

  def supplierAddToQueue: IO[Unit] = for {
    _ <- debugIO("acquiring semaphore") *> conveyorSemaphore.acquire *> debugIO("acquired semaphore")
    component <- supplier.supplyIO
    _ <- dequeue.put(component)
    _ <- debugIO("releasing semaphore") *> conveyorSemaphore.release *> debugIO("released semaphore")
  } yield ()

  def checkForStaleComponent(dequeue: PeekableDequeue[IO, Component]): IO[Unit] = for {
    _ <- debugIO("checking for a stale component")
    now <- IO(Calendar.getInstance())
    lastTake <- lastTakeRef.get
    _ <- Monad[IO].whenA((now.getTimeInMillis - lastTake.getTimeInMillis) > factoryConfig.inactivityTimeout.toMillis){
      for {
        _ <- dequeue.take.flatMap(throwAway => infoIO(s"Supplier threw stale $throwAway away!"))
      } yield ()
    }
  } yield ()


}
object Factory{
  def factoryIO(conveyorSemaphore: Semaphore[IO],
                queue: PeekableDequeue[IO, Component],
                lastTakeRef: Ref[IO, Calendar],
                producer: Supplier,
                consumers: Seq[AssemblerRobot],
                factoryConfig: FactoryConfig
             ): IO[Factory] = IO(Factory(conveyorSemaphore, queue, lastTakeRef, producer, consumers, factoryConfig))
}

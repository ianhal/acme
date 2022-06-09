package com.acme
package factory

import consumer.AssemblerRobot
import domain.Component
import producer.Supplier

import cats.Monad
import cats.effect.std.Semaphore
import cats.effect.{IO, Ref}
import com.acme.config.AcmeConfig.FactoryConfig
import com.acme.utils.LogSupportIO

import java.util.Calendar
import scala.concurrent.duration.DurationInt

case class Factory(semaphore: Semaphore[IO], dequeue: PeekableDequeue[IO, Component], lastTakeRef: Ref[IO, Calendar])(supplier: Supplier, consumers: Seq[AssemblerRobot], factoryConfig: FactoryConfig) extends LogSupportIO {

  import utils.Rich.RichIO

  import cats.syntax.parallel._

  val inactivityTimeout = factoryConfig.inactivityTimeout.toMillis

  def startIO: IO[Unit] = for {
    _ <- infoIO("Starting Factory")
    _ <- supplierAddToQueue.loopForever.start.map(_.cancel)
    _ <- consumers.map(_.startIO()).parSequence
    - <- checkForStaleComponent(dequeue).notFasterThan(2.seconds).loopForever.start.map(_.cancel)
  } yield ()

  def supplierAddToQueue: IO[Unit] = for {
    _ <- debugIO("acquiring") *> semaphore.acquire *> debugIO("acquired")
    component <- supplier.supplyIO
    _ <- dequeue.put(component)
    _ <- debugIO("releasing") *> semaphore.release *> debugIO("released")
  } yield ()

  def checkForStaleComponent(dequeue: PeekableDequeue[IO, Component]): IO[Unit] = for {
    _ <- debugIO("check stale")
    now <- IO(Calendar.getInstance())
    lastTake <- lastTakeRef.get
    _ <- Monad[IO].whenA(now.getTimeInMillis - lastTake.getTimeInMillis > factoryConfig.inactivityTimeout.toMillis){
      for {
        _ <- dequeue.take.flatMap(throwAway => infoIO(s"Supplier threw stale $throwAway away!"))
      } yield ()
    }
  } yield ()


}
object Factory{
  def startIO(semaphore: Semaphore[IO], queue: PeekableDequeue[IO, Component], lastTakeRef: Ref[IO, Calendar])(producer: Supplier, consumers: Seq[AssemblerRobot], factoryConfig: FactoryConfig): IO[Unit] = Factory(semaphore, queue, lastTakeRef)(producer, consumers, factoryConfig).startIO
}

package com.acme
package producer

import config.AcmeConfig.SupplierConfig
import domain.Component
import factory.PeekableDequeue
import producer.Supplier.AVAILABLE_COMPONENTS
import utils.{LogSupportIO, Rich}

import cats.Monad
import cats.effect.std.Semaphore
import cats.effect.{IO, Ref}

import java.util.Calendar
import scala.concurrent.duration.DurationInt

case class Supplier(lastTakeRef: Ref[IO, Calendar],
                    dequeue: PeekableDequeue[IO, Component],
                    conveyorSemaphore: Semaphore[IO],
                    supplierConfig: SupplierConfig
                   ) extends LogSupportIO {

  import Rich._

  private val INACTIVITY_TIMEOUT: Long = supplierConfig.inactivityTimeout.toMillis

  def startIO: IO[Unit] = for {
    _ <- infoIO("starting")
    _ <- supplierAddToQueue.loopForever.start.map(_.cancel)
    _ <- checkForStaleComponentIO.notFasterThan(2.seconds).loopForever.start.map(_.cancel)
  } yield ()

  def supplierAddToQueue: IO[Unit] = for {
    _ <- debugIO("acquiring semaphore") *> conveyorSemaphore.acquire *> debugIO("acquired semaphore")
    component <- supplyComponentIO
    _ <- dequeue.put(component)
    _ <- debugIO("releasing semaphore") *> conveyorSemaphore.release *> debugIO("released semaphore")
  } yield ()

  def supplyComponentIO: IO[Component] = IO {
    val component = AVAILABLE_COMPONENTS.maybeRandom.get
    info(s"added to conveyor - $component")
    component
  }.notFasterThan(supplierConfig.buildTime)

  def checkForStaleComponentIO: IO[Unit] = for {
    _ <- debugIO("checking for a stale component")
    now <- IO(Calendar.getInstance())
    lastTake <- lastTakeRef.get
    _ <- Monad[IO].whenA((now.getTimeInMillis - lastTake.getTimeInMillis) > INACTIVITY_TIMEOUT){
      for {
        _ <- dequeue.take.flatMap(throwAway => infoIO(s"disposed stale - $throwAway"))
      } yield ()
    }
  } yield ()
}

object Supplier {

  val AVAILABLE_COMPONENTS = List(
    Component.MainUnit,
    Component.Mop,
    Component.Broom
  )

  def createIO(lastTakeRef: Ref[IO, Calendar],
               dequeue: PeekableDequeue[IO, Component],
               conveyorSemaphore: Semaphore[IO],
               supplierConfig: SupplierConfig): IO[Supplier] = IO(Supplier(lastTakeRef, dequeue, conveyorSemaphore, supplierConfig))
}

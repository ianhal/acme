package com.acme
package producer

import config.AcmeConfig.SupplierConfig
import domain.Component
import producer.Supplier.AVAILABLE_COMPONENTS
import utils.{LogSupportIO, Rich}

import cats.Monad
import cats.effect.{IO, Ref}
import com.acme.factory.PeekableDequeue

import java.util.Calendar

case class Supplier(lastTakeRef: Ref[IO, Calendar], supplierConfig: SupplierConfig) extends LogSupportIO {

  import Rich._

  private val INACTIVITY_TIMEOUT: Long = supplierConfig.inactivityTimeout.toMillis

  def supplyComponentIO: IO[Component] = IO {
    val component = AVAILABLE_COMPONENTS.maybeRandom.get
    info(s"Supplier added to conveyor belt: $component")
    component
  }.notFasterThan(supplierConfig.buildTime)

  def checkForStaleComponentIO(dequeue: PeekableDequeue[IO, Component]): IO[Unit] = for {
    _ <- debugIO("checking for a stale component")
    now <- IO(Calendar.getInstance())
    lastTake <- lastTakeRef.get
    _ <- Monad[IO].whenA((now.getTimeInMillis - lastTake.getTimeInMillis) > INACTIVITY_TIMEOUT){
      for {
        _ <- dequeue.take.flatMap(throwAway => infoIO(s"Supplier disposed of a stale component: $throwAway!"))
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

  def createIO(lastTakeRef: Ref[IO, Calendar], supplierConfig: SupplierConfig): IO[Supplier] = IO(Supplier(lastTakeRef, supplierConfig))
}

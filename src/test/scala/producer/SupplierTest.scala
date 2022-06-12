package com.acme
package producer

import domain.Component
import factory.PeekableDequeue

import cats.effect.std.Semaphore
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import java.util.Calendar

class SupplierTest extends FactoryTestSupport {

  test("Supplier creates a component successfully"){
    val component = (for{
      lastTakeRef <- Ref[IO].of(Calendar.getInstance())
      dequeue <- PeekableDequeue[IO].create[Component](lastTakeRef, factoryConfig)
      conveyorSemaphore <- Semaphore[IO](1)
      supplier <- Supplier.createIO(lastTakeRef, dequeue, conveyorSemaphore, supplierConfig)
      component <- supplier.supplyComponentIO
    } yield component).unsafeRunSync

    assert(Supplier.AVAILABLE_COMPONENTS.contains(component), "Supplier should be able to create an available component")
  }

  test("supplier disposes of stale component after 10 seconds"){
    val (takenFromFront, createdComponent2) = (for{
      lastTakeRef <- Ref[IO].of(Calendar.getInstance())
      dequeue <- PeekableDequeue[IO].create[Component](lastTakeRef, factoryConfig)
      conveyorSemaphore <- Semaphore[IO](1)
      supplier <- Supplier.createIO(lastTakeRef, dequeue, conveyorSemaphore, supplierConfig.copy(inactivityTimeout = 1.second))
      createdComponent1 <- supplier.supplyComponentIO
      createdComponent2 <- supplier.supplyComponentIO
      createdComponent3 <- supplier.supplyComponentIO
      _ <- dequeue.put(createdComponent1)
      _ <- dequeue.put(createdComponent2)
      _ <- dequeue.put(createdComponent3)
      _ <- IO.sleep(2.seconds)
      _ <- supplier.checkForStaleComponentIO
      takenFromFront <- dequeue.take
    } yield (takenFromFront, createdComponent2)).unsafeRunSync

    assert(takenFromFront === createdComponent2, "supplier should throw out stale 1st component and leave 2nd in front of queue")
  }
}

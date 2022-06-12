package com.acme
package consumer

import config.AcmeConfig.ConsumerConfig
import domain.Component
import factory.PeekableDequeue
import utils.LogSupportIO

import cats.Monad
import cats.effect.std.Semaphore
import cats.effect.{IO, Ref}

case class Prerequisite(Component: Component, inInventory: Boolean)

abstract class AssemblerRobot(conveyorSemaphore: Semaphore[IO], dequeue: PeekableDequeue[IO, Component], consumerConfig: ConsumerConfig) extends LogSupportIO {

  import utils.Rich._

  protected val prerequisites: Map[Component, Int]
  protected val product: String

  def startIO(): IO[Unit] = for {
    currentInventoryRef <- Ref[IO].of[Map[Component, Int]](Map.empty[Component, Int])
    buildCountRef <- Ref[IO].of[Int](0)
    _ <- infoIO(s"starting")
    _ <- fetchAndAttemptBuildIO(currentInventoryRef, buildCountRef).notFasterThan(consumerConfig.retrievalTimeMS).loopForever.start.map(_.cancel)
  } yield ()

  def fetchAndAttemptBuildIO(currentInventoryRef: Ref[IO, Map[Component, Int]], buildCountRef: Ref[IO, Int]): IO[Unit] = for {
    _ <- debugIO("acquiring semaphore") *> conveyorSemaphore.acquire *> debugIO("acquired semaphore")
    maybePeeked <- dequeue.tryPeek
    preInventory <- currentInventoryRef.get
    _ <- Monad[IO].whenA(maybePeeked.nonEmpty && needsComponent(maybePeeked.get, preInventory)){
      dequeue.take.flatMap(updateInventory(currentInventoryRef)) *> infoIO(s"fetched from conveyor - ${maybePeeked.get}")
    }
    _ <- debugIO("releasing semaphore") *> conveyorSemaphore.release *> debugIO("released semaphore")
    postInventory <- currentInventoryRef.get
    _ <- debugIO(s"${getClass.getSimpleName} Inventory: $postInventory")
    _ <- Monad[IO].whenA(hasAllPrerequisites(postInventory)) {
      buildIO(currentInventoryRef, buildCountRef).notFasterThan(consumerConfig.assemblyTime)
    }
  } yield ()

  private def updateInventory(currentInventoryRef: Ref[IO, Map[Component, Int]])(component: Component): IO[Unit] =
    currentInventoryRef.update(ci => {
      ci.updatedWith(component)(mc => mc.fold[Option[Int]](Some(1))(c => Some(c + 1)))
    })

  def hasAllPrerequisites(currentInventory: Map[Component, Int]): Boolean = {
    currentInventory.equals(prerequisites)
  }

  def needsComponent(component: Component, currentInventory: Map[Component, Int]): Boolean = {
    (prerequisites.get(component), currentInventory.get(component)) match {
      case (None, _) => false
      case (Some(_), None) => true
      case (Some(pCount), Some(iCount)) if pCount > iCount => true
      case _ => false
    }


  }

  def buildIO(currentInventoryRef: Ref[IO, Map[Component, Int]], buildCountRef: Ref[IO, Int]): IO[Unit] = for {
      _ <- currentInventoryRef.update(_ => Map.empty[Component, Int])
      previousCount <- buildCountRef.get
      _ <- IO(previousCount + 1).flatMap { newCount =>
          infoIO(s"created - $product - build count: $newCount ") *> buildCountRef.update(_ => newCount)
        }
  } yield ()

}

object AssemblerRobot {

  def createWetRobotIO(dequeue: PeekableDequeue[IO, Component], conveyorSemaphore: Semaphore[IO], consumerConfig: ConsumerConfig): IO[AssemblerRobot] = IO(WetRobot(conveyorSemaphore, dequeue, consumerConfig))
  def createDryRobotIO(dequeue: PeekableDequeue[IO, Component], conveyorSemaphore: Semaphore[IO], consumerConfig: ConsumerConfig): IO[AssemblerRobot] = IO(DryRobot(conveyorSemaphore, dequeue, consumerConfig))
}

case class WetRobot(conveyorSemaphore: Semaphore[IO], dequeue: PeekableDequeue[IO, Component], consumerConfig: ConsumerConfig) extends AssemblerRobot(conveyorSemaphore, dequeue, consumerConfig) {

  override protected val prerequisites: Map[Component, Int] = Map(
    Component.MainUnit -> 1,
    Component.Mop -> 2
  )
  override protected val product: String = "Wet-2000"
}
case class DryRobot(conveyorSemaphore: Semaphore[IO], dequeue: PeekableDequeue[IO, Component], consumerConfig: ConsumerConfig) extends AssemblerRobot(conveyorSemaphore, dequeue, consumerConfig) {

  override protected val prerequisites: Map[Component, Int] = Map(
    Component.MainUnit -> 1,
    Component.Broom -> 2
  )
  override protected val product: String = "Dry-2000"
}

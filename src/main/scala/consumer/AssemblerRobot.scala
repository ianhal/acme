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
    currentInventoryRef <- Ref[IO].of[List[Component]](List.empty[Component])
    buildCountRef <- Ref[IO].of[Int](0)
    _ <- infoIO(s"starting AssemblerRobot[${getClass.getSimpleName}]")
    _ <- fetchAndAttemptBuildIO(currentInventoryRef, buildCountRef).notFasterThan(consumerConfig.retrievalTimeMS).loopForever.start.map(_.cancel)
  } yield ()

  def fetchAndAttemptBuildIO(currentInventoryRef: Ref[IO, List[Component]], buildCountRef: Ref[IO, Int]): IO[Unit] = for {
    _ <- debugIO("acquiring semaphore") *> conveyorSemaphore.acquire *> debugIO("acquired semaphore")
    maybePeeked <- dequeue.tryPeek
    preInventory <- currentInventoryRef.get
    _ <- Monad[IO].whenA(maybePeeked.nonEmpty && needsComponent(maybePeeked.get, preInventory)){
      dequeue.take.flatMap(d => currentInventoryRef.update(l => l :+ d)) *> infoIO(s"${getClass.getSimpleName} took from conveyor belt: ${maybePeeked.get}")
    }
    _ <- debugIO("releasing semaphore") *> conveyorSemaphore.release *> debugIO("released semaphore")
    postInventory <- currentInventoryRef.get
    _ <- debugIO(s"${getClass.getSimpleName} Inventory: $postInventory")
    _ <- Monad[IO].whenA(hasAllPrerequisites(postInventory)) {
      buildIO(currentInventoryRef, buildCountRef).notFasterThan(consumerConfig.assemblyTime)
    }
  } yield ()

  def hasAllPrerequisites(currentInventory: List[Component]): Boolean = {
    val componentCounts = currentInventory.groupBy(identity).view.mapValues(_.size).toMap
    componentCounts.equals(prerequisites)
  }

  def needsComponent(component: Component, currentInventory: List[Component]): Boolean = {
    val count = currentInventory.count(_ == component)
    prerequisites.get(component).exists(pCount => count < pCount)
  }

  def buildIO(currentInventoryRef: Ref[IO, List[Component]], buildCountRef: Ref[IO, Int]): IO[Unit] = for {
      _ <- currentInventoryRef.update(_ => List.empty[Component])
      previousCount <- buildCountRef.get
      _ <- IO(previousCount + 1).flatMap { newCount =>
          infoIO(s"building...building...building...$product created. Build count: $newCount ") *> buildCountRef.update(_ => newCount)
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

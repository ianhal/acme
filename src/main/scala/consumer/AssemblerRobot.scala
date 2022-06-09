package com.acme
package consumer

import config.AcmeConfig.ConsumerConfig
import domain.Component
import factory.PeekableDequeue
import utils.LogSupportIO

import cats.Monad
import cats.effect.std.Semaphore
import cats.effect.{IO, Ref}
import com.acme.Main.infoIO

import scala.concurrent.duration.DurationInt

case class Prerequisite(Component: Component, inInventory: Boolean)

abstract class AssemblerRobot(semaphore: Semaphore[IO], dequeue: PeekableDequeue[IO, Component], consumerConfig: ConsumerConfig) extends LogSupportIO {

  import utils.Rich._

  protected val prerequisites: Map[Component, Int]
  protected val buildText: String

  def startIO(): IO[Unit] = for {
    currentInventoryRef <- Ref[IO].of[List[Component]](List.empty[Component])
    buildCountRef <- Ref[IO].of[Int](0)
    _ <- infoIO(s"starting AssemblerRobot[${getClass.getSimpleName}]")
    _ <- fetchAndAttemptBuildIO(currentInventoryRef, buildCountRef).notFasterThan(1.seconds).loopForever.start.map(_.cancel)
  } yield ()

  def fetchAndAttemptBuildIO(currentInventoryRef: Ref[IO, List[Component]], buildCountRef: Ref[IO, Int]): IO[Unit] = for {
    _ <- debugIO("acquiring") *> semaphore.acquire *> debugIO("acquired")
    _ <- debugIO("before peak")
    maybePeeked <- dequeue.tryPeek
    _ <- debugIO("after peak")
    preInventory <- currentInventoryRef.get
    _ <- debugIO("before needsComponent")
    _ <- Monad[IO].whenA(maybePeeked.nonEmpty && needsComponent(maybePeeked.get, preInventory)){
      debugIO("before dequeue take") *> dequeue.take.flatMap(d => currentInventoryRef.update(l => l :+ d)) *> infoIO(s"${getClass.getSimpleName} took ${maybePeeked.get} from conveyor belt.")
    }
    _ <- debugIO("after needsComponent")
    _ <- debugIO("releasing") *> semaphore.release *> debugIO("released")
    postInventory <- currentInventoryRef.get
    - <- infoIO(s"${getClass.getSimpleName} Inventory: $postInventory")
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
          infoIO(s"$buildText. Build count: $newCount ") *> buildCountRef.update(_ => newCount)
        }
  } yield ()

}

object AssemblerRobot {

  def createWetRobotIO(dequeue: PeekableDequeue[IO, Component], sem: Semaphore[IO], consumerConfig: ConsumerConfig): IO[AssemblerRobot] = IO(WetRobot(sem, dequeue, consumerConfig))
  def createDryRobotIO(dequeue: PeekableDequeue[IO, Component], sem: Semaphore[IO], consumerConfig: ConsumerConfig): IO[AssemblerRobot] = IO(DryRobot(sem, dequeue, consumerConfig))
}

case class WetRobot(semaphore: Semaphore[IO], dequeue: PeekableDequeue[IO, Component], consumerConfig: ConsumerConfig) extends AssemblerRobot(semaphore, dequeue, consumerConfig) {

  override protected val prerequisites: Map[Component, Int] = Map(
    Component.MainUnit -> 1,
    Component.Mop -> 2
  )
  override protected val buildText: String = "Wet-2000 created"
}
case class DryRobot(semaphore: Semaphore[IO], dequeue: PeekableDequeue[IO, Component], consumerConfig: ConsumerConfig) extends AssemblerRobot(semaphore, dequeue, consumerConfig) {

  override protected val prerequisites: Map[Component, Int] = Map(
    Component.MainUnit -> 1,
    Component.Broom -> 2
  )
  override protected val buildText: String = "Dry-2000 created"
}

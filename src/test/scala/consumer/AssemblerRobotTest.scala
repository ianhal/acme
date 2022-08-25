package com.acme
package consumer

import config.AcmeConfig.ConsumerConfig
import domain.Component
import factory.PeekableDequeue

import cats.effect.std.Semaphore
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}

import java.util.Calendar

class AssemblerRobotTest extends FactoryTestSupport {

  test("WetRobot collects 2 x Mops, 1 Mainunit, builds, and then has a buildCount of 1"){
    val queueInput = IO((
      Component.Mop,
      Component.Mop,
      Component.MainUnit
    ))

    val (buildCount, currentInventory) = createRobotAndReturnState(queueInput, AssemblerRobot.createWetRobotIO)

    assert(buildCount == 1, "only one device was built and the count should correspond to this")
    assert(currentInventory.isEmpty, "the inventory should be used up by the AssemblyRobot")
  }

  test("WetRobot collects 1 Mop, 1 Mainunit, cannot build, and then has a buildCount of 0"){
    val queueInput = IO((
      Component.Mop,
      Component.MainUnit,
      Component.Broom
    ))

    val (buildCount, currentInventory) = createRobotAndReturnState(queueInput, AssemblerRobot.createWetRobotIO)

    assert(buildCount == 0, "nothing can be built because not enough prerequisites")
    assert(currentInventory.size == 2, "inventory should still be present")
  }

  test("DryRobot collects 2 x Brooms, 1 Mainunit, builds, and then has a buildCount of 1"){
    val queueInput = IO((
      Component.Broom,
      Component.Broom,
      Component.MainUnit
    ))

    val (buildCount, currentInventory) = createRobotAndReturnState(queueInput, AssemblerRobot.createDryRobotIO)

    assert(buildCount == 1, "only one device was built and the count should correspond to this")
    assert(currentInventory.isEmpty, "the inventory should be used up by the AssemblyRobot")
  }

  test("DryRobot collects 1 Broom, 1 Mainunit, cannot build, and then has a buildCount of 0"){
    val queueInput = IO((
      Component.Broom,
      Component.MainUnit,
      Component.Mop
    ))

    val (buildCount, currentInventory) = createRobotAndReturnState(queueInput, AssemblerRobot.createDryRobotIO)

    assert(buildCount == 0, "nothing can be built because not enough prerequisites")
    assert(currentInventory.size == 2, "inventory should still be still present")
  }

  private def createRobotAndReturnState(queueInput: IO[(Component, Component, Component)], robotFunc: (PeekableDequeue[Component], Semaphore[IO], ConsumerConfig)=> IO[AssemblerRobot]): (Int, Map[Component, Int]) = {

    val (buildCount, currentInventory) = (for {
      currentInventoryRef <- Ref[IO].of[Map[Component, Int]](Map.empty[Component, Int])
      buildCountRef <- Ref[IO].of[Int](0)
      lastTimeRef <- Ref[IO].of[Calendar](Calendar.getInstance())
      queue <- PeekableDequeue.create[Component](lastTimeRef, factoryConfig)
      semaphore <- Semaphore[IO](1)
      qInput <- queueInput
      _ <- queue.put(qInput._1)
      _ <- queue.put(qInput._2)
      _ <- queue.put(qInput._3)
      robot <- robotFunc(queue, semaphore, consumerConfig)
      _ <- robot.fetchAndAttemptBuildIO(currentInventoryRef, buildCountRef)
      _ <- robot.fetchAndAttemptBuildIO(currentInventoryRef, buildCountRef)
      _ <- robot.fetchAndAttemptBuildIO(currentInventoryRef, buildCountRef)
      buildCount <- buildCountRef.get
      currentInventory <- currentInventoryRef.get
    } yield (buildCount, currentInventory)).unsafeRunSync

    (buildCount, currentInventory)
  }

}

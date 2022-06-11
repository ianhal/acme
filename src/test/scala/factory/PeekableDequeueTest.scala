package com.acme
package factory

import config.AcmeConfig

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import java.util.Calendar

class PeekableDequeueTest extends AnyFunSuite{

  val config = AcmeConfig.fromConfig(ConfigFactory.load()).factoryConfig

  test("taking from front of queue"){

    val (peek1, peek2) = (for {
      lastTimeRef <- Ref[IO].of[Calendar](Calendar.getInstance())
      queue <- PeekableDequeue[IO].create[Int](config, lastTimeRef)
      _ <- queue.put(2)
      _ <- queue.put(1)
      p1 <- queue.take
      p2 <- queue.take
    } yield (p1, p2)).unsafeRunSync

    assert(peek1 == 2, "should take the Front of queue")
  }

  test("peeking multiple times gives same value"){
    val (peek1, peek2) = (for {
      lastTimeRef <- Ref[IO].of[Calendar](Calendar.getInstance())
      queue <- PeekableDequeue[IO].create[Int](config, lastTimeRef)
      _ <- queue.put(2)
      _ <- queue.put(1)
      p1 <- queue.peek
      p2 <- queue.peek
    } yield (p1, p2)).unsafeRunSync

    assert(peek1 == peek2, "peek1 and peek2 should be the same as peek put back in front after taking")
  }

  test("dequeue lastTakeTime changes after takes"){

    val (firstTakeTime, secondTakeTime) = (for {
      lastTimeRef <- Ref[IO].of[Calendar](Calendar.getInstance())
      queue <- PeekableDequeue[IO].create[Int](config, lastTimeRef)
      _ <- queue.put(2)
      _ <- queue.put(1)
      _ <- queue.take
      firstTakeTime <- lastTimeRef.get
      _ <- IO.sleep(1.second)
      _ <- queue.take
      secondTakeTime <- lastTimeRef.get

    } yield (firstTakeTime, secondTakeTime)).unsafeRunSync()

    assert(secondTakeTime.after(firstTakeTime), "last take time should change triggered from take execution")
  }

}

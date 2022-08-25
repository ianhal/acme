package com.acme
package factory

import config.AcmeConfig.FactoryConfig

import cats.Monad
import cats.effect.{IO, Ref}

import java.util.Calendar

abstract class PeekableDequeue[A] {

  def put(a: A): IO[Unit]

  def take: IO[A]

  def peek: IO[A]

  def tryPut(a: A): IO[Boolean]

  def tryTake: IO[Option[A]]

  def tryPeek: IO[Option[A]]

}

object PeekableDequeue {
  import cats.effect.Concurrent
  import cats.effect.std.Dequeue

  class PeekableQueueBuilder(private val concurrentIO: Concurrent[IO]) extends AnyVal {

    private implicit def _IO: Concurrent[IO] = concurrentIO

    def create[A](lastTakeRef: Ref[IO, Calendar], config: FactoryConfig): IO[PeekableDequeue[A]] = Dequeue.bounded[IO, A](config.queueSize).map(dequeue => peekableQueue[A](dequeue, lastTakeRef))

    private def peekableQueue[A](q: cats.effect.std.Dequeue[IO, A], lastTakeRef: Ref[IO, Calendar]): PeekableDequeue[A] =
      new PeekableDequeue[A]  {

        override def put(a: A): IO[Unit] = q.offer(a)

        override def take: IO[A] = for {
          take <- q.take
          _ <- lastTakeRef.update(_ => Calendar.getInstance())
        } yield take

        override def tryPut(a: A): IO[Boolean] = q.tryOffer(a)

        override def tryTake: IO[Option[A]] = for {
          tryTake <- q.tryTake
          _ <- lastTakeRef.update(_ => Calendar.getInstance())
        } yield tryTake

        override def peek: IO[A] = for {
          peeked <- q.take
          _ <- q.offerFront(peeked)
        } yield peeked

        override def tryPeek: IO[Option[A]] = for {
          peeked <- q.tryTake
          _ <- Monad[IO].whenA(peeked.nonEmpty){
            q.offerFront(peeked.get)
          }
        } yield peeked
      }

  }

  def apply(implicit concurrentIO: Concurrent[IO]): PeekableQueueBuilder = new PeekableQueueBuilder(concurrentIO)
  def create[A](lastTakeRef: Ref[IO, Calendar], config: FactoryConfig)(implicit concurrentIO: Concurrent[IO]): IO[PeekableDequeue[A]] =
    new PeekableQueueBuilder(concurrentIO).create[A](lastTakeRef, config)
}

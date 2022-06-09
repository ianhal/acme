package com.acme
package factory

import config.AcmeConfig.FactoryConfig

import cats.Monad
import cats.effect.Ref

import java.util.Calendar

abstract class PeekableDequeue[F[_], A] {

  def put(a: A): F[Unit]

  def take: F[A]

  def peek: F[A]

  def tryPut(a: A): F[Boolean]

  def tryTake: F[Option[A]]

  def tryPeek: F[Option[A]]

}

object PeekableDequeue {
  import cats.effect.Concurrent
  import cats.effect.std.Dequeue
  import cats.syntax.all._

  class PeekableQueueBuilder[F[_]](private val F: Concurrent[F]) extends AnyVal {

    private implicit def _F: Concurrent[F] = F

    def create[A](config: FactoryConfig, lastTakeRef: Ref[F, Calendar]): F[PeekableDequeue[F, A]] = Dequeue.bounded[F, A](config.queueSize).map(dequeue => peekableQueue[A](dequeue, lastTakeRef))

    private def peekableQueue[A](q: cats.effect.std.Dequeue[F, A], lastTakeRef: Ref[F, Calendar]): PeekableDequeue[F, A] =
      new PeekableDequeue[F, A]  {

        override def put(a: A): F[Unit] = q.offer(a)

        override def take: F[A] = for {
          take <- q.take
          _ <- lastTakeRef.update(_ => Calendar.getInstance())
        } yield take

        override def tryPut(a: A): F[Boolean] = q.tryOffer(a)

        override def tryTake: F[Option[A]] = for {
          tryTake <- q.tryTake
          _ <- lastTakeRef.update(_ => Calendar.getInstance())
        } yield tryTake

        override def peek: F[A] = for {
          peeked <- q.take
          _ <- q.offerFront(peeked)
        } yield peeked

        override def tryPeek: F[Option[A]] = for {
          peeked <- q.tryTake
          _ <- Monad[F].whenA(peeked.nonEmpty){
            q.offerFront(peeked.get)
          }
        } yield peeked
      }

  }

  def apply[F[_]](implicit F: Concurrent[F]): PeekableQueueBuilder[F] = new PeekableQueueBuilder[F](F)

}

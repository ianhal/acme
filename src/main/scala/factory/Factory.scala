package com.acme
package factory

import config.AcmeConfig.FactoryConfig
import consumer.AssemblerRobot
import producer.Supplier
import utils.LogSupportIO

import cats.effect.IO

case class Factory(supplier: Supplier,
                   consumers: Seq[AssemblerRobot],
                   factoryConfig: FactoryConfig
                  ) extends LogSupportIO {

  import cats.syntax.parallel._

  def startIO: IO[Unit] = for {
    _ <- infoIO("Starting Factory")
    _ <- supplier.startIO
    _ <- consumers.map(_.startIO()).parSequence
  } yield ()

}
object Factory{
  def factoryIO(supplier: Supplier,
                consumers: Seq[AssemblerRobot],
                factoryConfig: FactoryConfig
             ): IO[Factory] = IO(Factory(supplier, consumers, factoryConfig))
}

package com.acme
package producer

import config.AcmeConfig.SupplierConfig
import domain.Component
import producer.Supplier.AVAILABLE_COMPONENTS
import utils.{LogSupportIO, Rich}

import cats.effect.IO

case class Supplier(supplierConfig: SupplierConfig) extends LogSupportIO {

  import Rich._

  def supplyIO: IO[Component] = IO {
    val component = AVAILABLE_COMPONENTS.maybeRandom.get
    info(s"Supplier added to conveyor: $component")
    component
  }.notFasterThan(supplierConfig.buildTime)
}

object Supplier {

  val AVAILABLE_COMPONENTS = List(
    Component.MainUnit,
    Component.Mop,
    Component.Broom
  )

  def createIO(supplierConfig: SupplierConfig): IO[Supplier] = IO(Supplier(supplierConfig))
}

package com.acme
package producer

import config.AcmeConfig

import cats.effect.unsafe.implicits.global
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite

class SupplierTest extends AnyFunSuite{

  val config: AcmeConfig.SupplierConfig = AcmeConfig.fromConfig(ConfigFactory.load()).supplierConfig

  test("Supplier creates a component successfully"){
    val component = (for{
      supplier <- Supplier.create(config)
      component <- supplier.supplyIO
    } yield component).unsafeRunSync

    assert(Supplier.AVAILABLE_COMPONENTS.contains(component), "Supplier should be able to create an available component")
  }
}

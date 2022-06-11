package com.acme
package producer

import config.AcmeConfig

import cats.effect.{IO, Ref}
import cats.effect.unsafe.implicits.global
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite

import java.util.Calendar

class SupplierTest extends AnyFunSuite{

  val config: AcmeConfig.SupplierConfig = AcmeConfig.fromConfig(ConfigFactory.load()).supplierConfig

  test("Supplier creates a component successfully"){
    val component = (for{
      lastTakeRef <- Ref[IO].of(Calendar.getInstance())
      supplier <- Supplier.createIO(lastTakeRef, config)
      component <- supplier.supplyComponentIO
    } yield component).unsafeRunSync

    assert(Supplier.AVAILABLE_COMPONENTS.contains(component), "Supplier should be able to create an available component")
  }
}

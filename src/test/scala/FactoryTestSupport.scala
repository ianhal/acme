package com.acme

import config.AcmeConfig
import config.AcmeConfig.{ConsumerConfig, FactoryConfig, SupplierConfig}

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.funsuite.AnyFunSuite

abstract class FactoryTestSupport extends AnyFunSuite {
  val config: Config = ConfigFactory.load()
  val acmeConfig: AcmeConfig = AcmeConfig.fromConfig(ConfigFactory.load())
  val supplierConfig: SupplierConfig = acmeConfig.supplierConfig
  val factoryConfig: FactoryConfig = acmeConfig.factoryConfig
  val consumerConfig: ConsumerConfig = acmeConfig.consumerConfig

}

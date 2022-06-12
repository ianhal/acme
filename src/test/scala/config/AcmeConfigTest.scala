package com.acme
package config

import config.AcmeConfig.{ConsumerConfig, FactoryConfig, SupplierConfig}
import config.AcmeConfigTest._

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters._
import scala.util.Try

class AcmeConfigTest extends FactoryTestSupport {

  test("invalid SupplierConfig.inactivityTimeout fails conversion to FiniteDuration"){
    assertFiniteDurationFailure(createSupplierConfig(inactivityTimeout = "BOOM"))(FactoryConfig)
  }

  test("invalid ConsumerConfig.assemblyTime fails conversion to FiniteDuration"){
    assertFiniteDurationFailure(createConsumerConfig(assemblyTime = "BOOM"))(ConsumerConfig)
  }

  test("invalid SupplierConfig.buildTime fails conversion to FiniteDuration"){
    assertFiniteDurationFailure(createSupplierConfig(buildTime = "BOOM"))(SupplierConfig)
  }

  def assertFiniteDurationFailure[A](config: Config)(implicit C: SubConfig[A]): Unit = {
    val maybeConfig: Try[A] = Try {
      C.fromConfig(config)
    }
    assert(maybeConfig.isFailure, "FiniteDuration cannot parse configured value - it should be a number")
  }

}

object AcmeConfigTest {
  def createFctoryConfig(queueSize: String = "10"): Config = ConfigFactory.parseMap(
    Map(
      "queueSize" -> queueSize
    ).asJava)

  def createConsumerConfig(assemblyTime: String): Config = ConfigFactory.parseMap(
    Map(
      "assemblyTime" -> assemblyTime
    ).asJava)

  def createSupplierConfig(buildTime: String = "10", inactivityTimeout: String = "10"): Config = ConfigFactory.parseMap(
    Map(
      "buildTime" -> buildTime,
      "inactivityTimeout" -> inactivityTimeout
    ).asJava)
}

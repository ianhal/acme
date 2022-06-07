package com.acme
package config

import config.AcmeConfig.{ConsumerConfig, FactoryConfig}
import config.AcmeConfigTest._

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.funsuite.AnyFunSuite

import scala.jdk.CollectionConverters._
import scala.util.Try

class AcmeConfigTest extends AnyFunSuite {

  test("AcmeConfig build default values successfully"){
    val config = ConfigFactory.load()
    AcmeConfig.fromConfig(config)
  }

  test("valid FactoryConfig.inactivityTimeout fails conversion to FiniteDuration"){
    val config = factoryConfig()
    FactoryConfig.fromConfig(config)
  }

  test("invalid FactoryConfig.inactivityTimeout fails conversion to FiniteDuration"){
    assertFiniteDurationFailure(factoryConfig(inactivityTimeout = "BOOM"))(FactoryConfig)
  }

  test("invalid ConsumerConfig.assemblyTime fails conversion to FiniteDuration"){
    assertFiniteDurationFailure(consumerConfig(assemblyTime = "BOOM"))(ConsumerConfig)
  }

  test("invalid ProducerConfig.buildTime fails conversion to FiniteDuration"){
    assertFiniteDurationFailure(producerConfig(buildTime = "BOOM"))(ConsumerConfig)
  }

  def assertFiniteDurationFailure[A](config: Config)(implicit C: SubConfig[A]): Unit = {
    val maybeConfig: Try[A] = Try {
      C.fromConfig(config)
    }
    assert(maybeConfig.isFailure, "FiniteDuration cannot parse configured value - it should be a number")
  }

}

object AcmeConfigTest {
  def factoryConfig(queueSize: String = "10", inactivityTimeout: String = "10"): Config = ConfigFactory.parseMap(
    Map(
      "queueSize" -> queueSize,
      "inactivityTimeout" -> inactivityTimeout
    ).asJava)

  def consumerConfig(assemblyTime: String): Config = ConfigFactory.parseMap(
    Map(
      "assemblyTime" -> assemblyTime
    ).asJava)

  def producerConfig(buildTime: String): Config = ConfigFactory.parseMap(
    Map(
      "buildTime" -> buildTime
    ).asJava)
}

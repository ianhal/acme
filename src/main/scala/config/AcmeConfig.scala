package com.acme
package config

import config.AcmeConfig.{ConsumerConfig, FactoryConfig, SupplierConfig}

import cats.effect.IO
import com.typesafe.config.{Config, ConfigFactory}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

trait SubConfig[A]{
  def fromConfig(config: Config): A
}

case class AcmeConfig(
                       factoryConfig: FactoryConfig,
                       consumerConfig: ConsumerConfig,
                       supplierConfig: SupplierConfig
                     )
object AcmeConfig{
  import utils.Rich._

  def createIO: IO[AcmeConfig] = IO{
    val config = ConfigFactory.load()
    fromConfig(config)
  }

  def fromConfig(config: Config): AcmeConfig = {
    AcmeConfig(
      factoryConfig = FactoryConfig.fromConfig(config.getConfig("factory")),
      consumerConfig = ConsumerConfig.fromConfig(config.getConfig("consumer")),
      supplierConfig = SupplierConfig.fromConfig(config.getConfig("supplier"))
    )
  }

  case class FactoryConfig(queueSize: Int)
  object FactoryConfig extends SubConfig[FactoryConfig] {

    override def fromConfig(config: Config): FactoryConfig = {
      FactoryConfig(
        queueSize = config.getInt("queueSize")
      )
    }
  }

  case class ConsumerConfig(assemblyTime: FiniteDuration, retrievalTimeMS: FiniteDuration)
  object ConsumerConfig extends SubConfig[ConsumerConfig]{

    override def fromConfig(config: Config): ConsumerConfig = {
      ConsumerConfig(
        assemblyTime = config.getInt("assemblyTime").toFiniteDuration,
        retrievalTimeMS = config.getInt("retrievalTimeMS").toFiniteDuration(TimeUnit.MILLISECONDS)
      )
    }
  }

  case class SupplierConfig(buildTime: FiniteDuration, inactivityTimeout: FiniteDuration)

  object SupplierConfig extends SubConfig[SupplierConfig]{

    override def fromConfig(config: Config): SupplierConfig = {
      SupplierConfig(
        buildTime = config.getInt("buildTime").toFiniteDuration,
        inactivityTimeout = config.getInt("inactivityTimeout").toFiniteDuration
      )
    }
  }

}

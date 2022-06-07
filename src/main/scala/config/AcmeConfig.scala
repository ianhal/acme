package com.acme
package config

import config.AcmeConfig.{ConsumerConfig, FactoryConfig, ProducerConfig}

import cats.effect.IO
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.FiniteDuration

trait SubConfig[A]{
  def fromConfig(config: Config): A
}

case class AcmeConfig(
                       factoryConfig: FactoryConfig,
                       consumerConfig: ConsumerConfig,
                       producerConfig: ProducerConfig
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
      producerConfig = ProducerConfig.fromConfig(config.getConfig("producer"))
    )
  }

  case class FactoryConfig(queueSize: Int, inactivityTimeout: FiniteDuration)
  object FactoryConfig extends SubConfig[FactoryConfig] {

    override def fromConfig(config: Config): FactoryConfig = {
      FactoryConfig(
        queueSize = config.getInt("queueSize"),
        inactivityTimeout = config.getInt("inactivityTimeout").toFiniteDuration
      )
    }
  }

  case class ConsumerConfig(assemblyTime: FiniteDuration)
  object ConsumerConfig extends SubConfig[ConsumerConfig]{

    override def fromConfig(config: Config): ConsumerConfig = {
      ConsumerConfig(
        assemblyTime = config.getInt("assemblyTime").toFiniteDuration,
      )
    }
  }

  case class ProducerConfig(buildTime: FiniteDuration)

  object ProducerConfig extends SubConfig[ProducerConfig]{

    override def fromConfig(config: Config): ProducerConfig = {
      ProducerConfig(
        buildTime = config.getInt("buildTime").toFiniteDuration,
      )
    }
  }

}




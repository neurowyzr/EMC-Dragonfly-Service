package com.neurowyzr.nw.dragon.service.di

import javax.inject.Singleton

import com.twitter.inject.TwitterModule
import com.twitter.inject.annotations.Flag
import com.twitter.util.Duration

import com.neurowyzr.nw.dragon.service.cfg.Models.*
import com.neurowyzr.nw.dragon.service.di.annotations.*
import com.neurowyzr.nw.finatra.lib.cfg.Models.{AppInfo, BuildInfo}
import com.neurowyzr.nw.finatra.rabbitmq.lib.cfg.Models.{BrokerConfig, ConsumerConfig, PublisherConfig, TopologyConfig}

import com.google.inject.Provides
import pureconfig.{ConfigCursor, ConfigReader, ConfigSource, ConvertHelpers}
import pureconfig.ConfigConvert.catchReadError
import pureconfig.ConfigReader.Result
import pureconfig.configurable.genericMapReader
import pureconfig.generic.auto.*

object ConfigModule extends TwitterModule {

  // must not hold reference to a flag
  flag(name = "conf.file", default = "application.conf", help = "the configuration file."): Unit

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  @Provides
  @Singleton
  def providesAllConfig(@Flag("conf.file") confFile: String): AllConfig = {
    implicit val mqReader: ConfigReader[Map[String, Any]] = getMqMapReader
    implicit val durationReader: ConfigReader[Duration] = ConfigReader.fromString[Duration](
      ConvertHelpers.catchReadError(str => Duration.parse(str))
    )

    info("Loading configuration from " + confFile.toUpperCase())
    val configSource = ConfigSource.resources(confFile)
    configSource.loadOrThrow[AllConfig]
  }

  @SuppressWarnings(Array("org.wartremover.warts.IterableOps"))
  private def getMqMapReader: ConfigReader[Map[String, Any]] = {
    implicit val anyReader: ConfigReader[Any] =
      new ConfigReader[Any] {

        override def from(cur: ConfigCursor): Result[Any] = {
          val elems                     = cur.pathElems
          val (first, last, beforeLast) = (elems.last, elems.head, elems(elems.size - 2))

          (first, beforeLast, last) match {
            case ("mq", "topology", "x-queue-type") | ("mq", "topology", "x-dead-letter-exchange") =>
              Right(cur.valueOpt.map(_.unwrapped()).getOrElse(""))
            case ("mq", "topology", "x-message-ttl") =>
              val duration = cur.valueOpt.map(_.unwrapped().toString).getOrElse("")
              val parsed   = Duration.parse(duration)
              Right(parsed.inMilliseconds)
            case _ => Right("parser-not-defined")
          }
        }

      }

    genericMapReader[String, Any](catchReadError(_.toString))
  }

  @Provides
  @Singleton
  def providesAppConfig(allConfig: AllConfig): AppInfo = {
    allConfig.app
  }

  @Provides
  @Singleton
  def providesBuildInfo(allConfig: AllConfig): BuildInfo = {
    allConfig.build
  }

  @Provides
  @Singleton
  @CoreDatabaseBinding
  def providesCoreDatabaseConfig(allConfig: AllConfig): DatabaseConfig = {
    allConfig.coreDatabase
  }

  @Provides
  @Singleton
  @DragonDatabaseBinding
  def providesDragonflyDatabaseConfig(allConfig: AllConfig): DatabaseConfig = {
    allConfig.dragonDatabase
  }

  @Provides
  @Singleton
  def providesBrokerConfig(allConfig: AllConfig): BrokerConfig = {
    allConfig.mq.broker
  }

  @Provides
  @Singleton
  def providesConsumerConfig(allConfig: AllConfig): ConsumerConfig = {
    allConfig.mq.consumer
  }

  @Provides
  @Singleton
  @SelfPublisherBinding
  def providesPublisherConfig(allConfig: AllConfig): PublisherConfig = {
    allConfig.mq.selfPublisher
  }

  @Provides
  @Singleton
  @CygnusPublisherBinding
  def providesCygnusPublisherConfig(allConfig: AllConfig): PublisherConfig = {
    allConfig.mq.cygnusPublisher
  }

  @Provides
  @Singleton
  @EmailPublisherBinding
  def providesEmailPublisherConfig(allConfig: AllConfig): PublisherConfig = {
    allConfig.mq.emailPublisher
  }

  @Provides
  @Singleton
  def providesTopologyConfig(allConfig: AllConfig): TopologyConfig = {
    allConfig.mq.topology
  }

  @Provides
  @Singleton
  def providesDbfsConfig(allConfig: AllConfig): DbfsConfig = {
    allConfig.dbfs
  }

  @Provides
  @Singleton
  def providesCustomerConfig(allConfig: AllConfig): CustomerConfig = {
    allConfig.customer
  }

  @Provides
  @Singleton
  def providesCoreServiceConfig(allConfig: AllConfig): CoreServiceConfig = {
    allConfig.services.core
  }

  @Provides
  @Singleton
  def providesAlerterServiceConfig(allConfig: AllConfig): AlerterServiceConfig = {
    allConfig.services.alerter
  }

  @Provides
  @Singleton
  def providesCustomerServiceConfig(allConfig: AllConfig): CustomerServiceConfig = {
    allConfig.services.customer
  }

  @Provides
  @Singleton
  def providesAwsConfig(allConfig: AllConfig): AwsConfig = {
    allConfig.aws
  }

  @Provides
  @Singleton
  def providesAppTimezone(allConfig: AllConfig): AppTimezone = {
    allConfig.appTimezone
  }

}

package com.neurowyzr.nw.dragon.service.cfg

import com.twitter.util.Throw

import com.neurowyzr.nw.finatra.lib.cfg.Models.{AppInfo, BuildInfo, Sensitive}
import com.neurowyzr.nw.finatra.rabbitmq.lib.cfg.Models.*

object Models {

  type AppTimezone = String

  final case class AllConfig(app: AppInfo,
                             build: BuildInfo,
                             coreDatabase: DatabaseConfig,
                             dragonDatabase: DatabaseConfig,
                             mq: Messaging,
                             dbfs: DbfsConfig,
                             customer: CustomerConfig,
                             services: RemoteServices,
                             aws: AwsConfig,
                             appTimezone: AppTimezone
                            )

  final case class DatabaseConfig(
      source: String,
      name: String,
      host: String,
      port: Int,
      username: String,
      password: String
  )

  final case class Messaging(broker: BrokerConfig,
                             consumer: ConsumerConfig,
                             selfPublisher: PublisherConfig,
                             cygnusPublisher: PublisherConfig,
                             emailPublisher: PublisherConfig,
                             topology: TopologyConfig
                            )

  final case class DbfsConfig(newUserDefaultPassword: Sensitive,
                              daysTillExpiry: Int,
                              magicLinkPath: String,
                              otpValidityInMinutes: Int,
                              otpRetries: Int,
                              reportS3PublicPath: String
                             )

  final case class CustomerConfig(source: String, nwApiKey: String)

  final case class CoreServiceConfig(source: String, destination: String, address: String)

  final case class RemoteServices(core: CoreServiceConfig,
                                  alerter: AlerterServiceConfig,
                                  customer: CustomerServiceConfig
                                 )

  final case class AlerterServiceConfig(source: String, destination: String, address: String, args: Map[String, String])

  final case class CustomerServiceConfig(
      source: String,
      destination: String,
      address: String,
      username: String,
      password: String,
      httpMinRetryDelay: Int,
      httpMaxRetryDelay: Int,
      httpMaxRetries: Int
  )

  final case class S3Config(bucket: String)

  final case class AwsConfig(region: String, accessKey: String, accessSecret: Sensitive, storage: S3Config)

}

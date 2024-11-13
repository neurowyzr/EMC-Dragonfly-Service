package com.neurowyzr.nw.dragon.service.di

import java.util.concurrent.TimeUnit

import com.neurowyzr.nw.dragon.service.SharedFakes.{
  FakeAwsConfig, FakeCustomerConfig, FakeCustomerServiceConfig, FakeDbfsConfig, FakeRemoteServices
}
import com.neurowyzr.nw.dragon.service.cfg.Models.*
import com.neurowyzr.nw.dragon.service.di.ConfigModuleTest.*
import com.neurowyzr.nw.finatra.lib.cfg.Models.{AppInfo, BuildInfo}
import com.neurowyzr.nw.finatra.rabbitmq.lib.cfg.Models.*

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConfigModuleTest extends AnyWordSpec with Matchers {

  "providesAllConfig" should {
    "loads the configuration file when a config file is specified" in {
      val config = ConfigModule.providesAllConfig(confFile = "application.conf")
      config.app.name shouldBe "nw-apollo-dragon-service"
    }

    "loads the configuration file even if the topology is missing info" in {
      val config = ConfigModule.providesAllConfig(confFile = "application.incorrect.conf")
      val _      = config.app.name shouldBe "nw-apollo-dragon-service"

      val _ =
        config.mq.topology.queues.head.args.apply("incorrect-x-dead-letter-exchange") shouldBe "parser-not-defined"
      val _ = config.mq.topology.queues.head.args.apply("incorrect-x-queue-type") shouldBe "parser-not-defined"
      val _ = config.mq.topology.dlqs.head.args.apply("incorrect-x-message-ttl") shouldBe "parser-not-defined"
    }

    "throw when the specified config file does not exist" in {
      val exception = intercept[RuntimeException](ConfigModule.providesAllConfig(confFile = "does-not-exist.conf"))
      exception.getMessage should include("does-not-exist.conf")
    }
  }

  "providesAppConfig" should {
    "return the configuration" in {
      ConfigModule.providesAppConfig(FakeAllConfig) shouldBe FakeAppInfo
    }
  }

  "providesBuildInfo" should {
    "return the configuration" in {
      ConfigModule.providesBuildInfo(FakeAllConfig) shouldBe FakeBuildInfo
    }
  }

}

private object ConfigModuleTest {

  final val FakeAppInfo        = AppInfo("fake-name", "fake-version")
  final val FakeBuildInfo      = BuildInfo("fake-date", "fake-number")
  final val FakeDatabaseConfig = DatabaseConfig("fake-source", "fake-name", "fake-host", 0, "fake-username", "fake-pwd")

  final val FakeBrokerConfig = BrokerConfig("fake-mq", "fake-host", 5566, "fake-user", "fake-pwd", useSsl = false)

  final val FakeConsumerConfig = ConsumerConfig("fake-consumer",
                                                isEnabled = true,
                                                "fake-queue",
                                                1,
                                                isRequeuedToOriginal = false,
                                                com.twitter.util.Duration(10, TimeUnit.SECONDS)
                                               )

  final val FakePublisherConfig = PublisherConfig("fake-publisher",
                                                  isEnabled = false,
                                                  "fake-exchange",
                                                  "fake-routing-key",
                                                  isMandatory = false,
                                                  mustWaitForConfirm = false
                                                 )

  final val FakeTopologyConfig = TopologyConfig(
    Seq(ExchangeConfig("fake-exchange", "fake-type")),
    Seq(QueueConfig("fake-queue")),
    Seq(ExchangeConfig("fake-dlx", "fake-type")),
    Seq(QueueConfig("fake-dlq")),
    Seq.empty
  )

  final val FakeTimezone = "Asia/Singapore"

  final val FakeMessaging = Messaging(
    FakeBrokerConfig,
    FakeConsumerConfig,
    FakePublisherConfig,
    FakePublisherConfig,
    FakePublisherConfig,
    FakeTopologyConfig
  )

  final val FakeAllConfig = AllConfig(
    FakeAppInfo,
    FakeBuildInfo,
    FakeDatabaseConfig,
    FakeDatabaseConfig,
    FakeMessaging,
    FakeDbfsConfig,
    FakeCustomerConfig,
    FakeRemoteServices,
    FakeAwsConfig,
    FakeTimezone
  )

}

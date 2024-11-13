package com.neurowyzr.nw.dragon.service.mq

import java.util.Date

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.inject.app.TestInjector
import com.twitter.util.{Await, Future}
import com.twitter.util.jackson.ScalaObjectMapper

import com.neurowyzr.nw.dragon.service.biz.models.{Outcomes, TaskContext}
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.PatientNotFound
import com.neurowyzr.nw.dragon.service.mq.CygnusPublisherImplTest.FakeTaskContext
import com.neurowyzr.nw.dragon.service.mq.impl.CygnusPublisherImpl
import com.neurowyzr.nw.finatra.lib.cfg.Models.AppInfo
import com.neurowyzr.nw.finatra.rabbitmq.lib.{MqContext, MqPublisher}
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.Message
import com.neurowyzr.nw.finatra.rabbitmq.lib.cfg.Models.PublisherConfig

import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CygnusPublisherImplTest
    extends AnyWordSpec with IdiomaticMockito with Matchers with BeforeAndAfterEach with ResetMocksAfterEachTest {

  private val mockPublisher = mock[MqPublisher]
  private val mapper        = new ScalaObjectMapperModule()
  private val testInjector  = TestInjector(mapper).create()
  private val testMapper    = testInjector.instance[ScalaObjectMapper]
  private val mockMqContext = mock[MqContext]

  private val fakePublisherConfig = PublisherConfig("fake-label",
                                                    isEnabled = true,
                                                    "fake-exchange-name",
                                                    "fake-routing-key",
                                                    isMandatory = true,
                                                    mustWaitForConfirm = true
                                                   )

  private val fakeAppInfo = AppInfo("name", "0.1.0")

  private val fakeStatus = Outcomes.Success

  private def createTestInstance = new CygnusPublisherImpl(testMapper, mockMqContext, fakePublisherConfig, fakeAppInfo)

  "publish message" should {
    "succeed" in {
      val captor = ArgCaptor[Message]
      val _      = mockMqContext.createDefaultPublisher(*[PublisherConfig]) returns mockPublisher
      val _      = mockPublisher.publish(captor) returns Future {}

      val result: Unit = Await.result(createTestInstance.publishOutcome(FakeTaskContext, fakeStatus), 1.second)
      val _            = result shouldBe {}
      val _ = captor.value.body shouldBe testMapper.writeValueAsString(Ack(FakeTaskContext.`type`, "Success", ""))
    }

    "fail for unknown incoming commands" in {
      val _                      = mockMqContext.createDefaultPublisher(*[PublisherConfig]) returns mockPublisher
      val fakeInvalidTaskContent = FakeTaskContext.copy(maybeType = Some("unknown-command"))

      val result: Unit = Await.result(createTestInstance.publishOutcome(fakeInvalidTaskContent, fakeStatus), 1.second)
      val _            = result shouldBe {}
    }
  }

  "Printing Outcomes to string" should {
    "return 'success' for Outcomes.Success" in {
      val result = Outcomes.Success.toString

      val _ = result shouldBe "Success"
    }

    "return 'error' for Outcomes.Error" in {
      val result = PatientNotFound.toString

      val _ = result shouldBe "Error"
      val _ = PatientNotFound.getErrorCode shouldBe "101"
    }
  }

}

object CygnusPublisherImplTest {

  final val FakeTaskContext: TaskContext = TaskContext(
    Some(new Date()),
    Some("appId"),
    Some(CreateMagicLinkCmd.toString),
    Some("messageId"),
    Some("expiration"),
    Some("correlationId")
  )

}

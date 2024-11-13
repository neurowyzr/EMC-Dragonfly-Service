package com.neurowyzr.nw.dragon.service.mq

import java.util.Date

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.inject.app.TestInjector
import com.twitter.util.{Await, Future}
import com.twitter.util.jackson.ScalaObjectMapper

import com.neurowyzr.nw.dragon.service.biz.models.{EmailOtpArgs, TaskContext}
import com.neurowyzr.nw.dragon.service.mq.impl.EmailPublisherImpl
import com.neurowyzr.nw.finatra.lib.cfg.Models.AppInfo
import com.neurowyzr.nw.finatra.rabbitmq.lib.{MqContext, MqPublisher}
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.Message
import com.neurowyzr.nw.finatra.rabbitmq.lib.cfg.Models.PublisherConfig

import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EmailPublisherImplTest
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

  private def createTestInstance = new EmailPublisherImpl(testMapper, mockMqContext, fakePublisherConfig, fakeAppInfo)

  "publish message" should {
    "succeed" in {
      val captor = ArgCaptor[Message]
      val _      = mockMqContext.createDefaultPublisher(*[PublisherConfig]) returns mockPublisher
      val _      = mockPublisher.publish(captor) returns Future {}

      val emailOtpArgs = EmailOtpArgs("123456", "John")
      val email        = "email@email.com"

      val result: Unit = Await.result(createTestInstance.publishOtpEmail(emailOtpArgs, Set(email)), 1.second)
      val _            = result shouldBe {}

      mockMqContext.createDefaultPublisher(*[PublisherConfig]) wasCalled once
      mockMqContext wasNever calledAgain

      mockPublisher.publish(*[Message]) wasCalled once
      mockPublisher.close() wasCalled once
      mockPublisher wasNever calledAgain
    }

  }

}

object EmailPublisherImplTest {

  final val FakeTaskContext: TaskContext = TaskContext(
    Some(new Date()),
    Some("appId"),
    Some("EmailCmd"),
    Some("messageId"),
    Some("expiration"),
    Some("correlationId")
  )

}

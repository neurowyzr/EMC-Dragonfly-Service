package com.neurowyzr.nw.dragon.service.mq

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Date

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.inject.app.TestInjector
import com.twitter.util.{Await, Future}
import com.twitter.util.jackson.ScalaObjectMapper

import com.neurowyzr.nw.dragon.service.SharedFakes.{
  FakeCustomerConfig, FakedbfsConfig, FakeEpisodeRefAlpha, FakeExternalPatientRef, FakeLocationId, FakeRequestId
}
import com.neurowyzr.nw.dragon.service.WebService.injector
import com.neurowyzr.nw.dragon.service.biz.exceptions.PublisherException
import com.neurowyzr.nw.dragon.service.biz.models.{Outcome, TaskContext}
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.Success
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.EnqueueTestSessionParams
import com.neurowyzr.nw.dragon.service.cfg.Models.DbfsConfig
import com.neurowyzr.nw.dragon.service.mq.SelfPublisherImplTest.{
  ExpectedPublishFailurePayload, ExpectedPublishSuccessPayload, ExpectedTestSessionMessagePayload, FakeAppInfo,
  FakeMagicLinkUrl, FakeParams, FakePublisherConfig, FakePublishParams, FakeTaskContext
}
import com.neurowyzr.nw.dragon.service.mq.impl.SelfPublisherImpl
import com.neurowyzr.nw.dragon.service.mq.impl.SelfPublisherImpl.{EnqueueTestSessionMessagePayload, PublishParams}
import com.neurowyzr.nw.finatra.lib.cfg.Models.{AppInfo, Sensitive}
import com.neurowyzr.nw.finatra.rabbitmq.lib.{MqContext, MqPublisher}
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.Message
import com.neurowyzr.nw.finatra.rabbitmq.lib.cfg.Models.PublisherConfig

import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SelfPublisherImplTest
    extends AnyWordSpec with IdiomaticMockito with Matchers with BeforeAndAfterEach with ResetMocksAfterEachTest {

  private val mockPublisher = mock[MqPublisher]
  private val mapper        = new ScalaObjectMapperModule()
  private val testInjector  = TestInjector(mapper).create()
  private val testMapper    = testInjector.instance[ScalaObjectMapper]
  private val mockMqContext = mock[MqContext]

  private def createTestInstance =
    new SelfPublisherImpl(
      testMapper,
      mockMqContext,
      FakePublisherConfig,
      FakeAppInfo,
      FakeCustomerConfig,
      injector.instance(classOf[QueueConsumer]),
      FakedbfsConfig
    )

  "publish test session message" should {
    "succeed" in {
      val captor = ArgCaptor[Message]
      val _      = mockMqContext.createDefaultPublisher(*[PublisherConfig]) returns mockPublisher
      val _      = mockPublisher.publish(captor) returns Future {}

      val result: Unit = Await.result(createTestInstance.publishTestSessionMessage(FakeParams), 1.second)
      val _            = result shouldBe {}
      val _            = captor.value.body shouldBe testMapper.writeValueAsString(ExpectedTestSessionMessagePayload)
    }
  }

  "publish success message" should {
    "succeed" in {
      val captor = ArgCaptor[Message]
      val _      = mockMqContext.createDefaultPublisher(*[PublisherConfig]) returns mockPublisher
      val _      = mockPublisher.publish(captor) returns Future {}

      val result: Unit = Await.result(
        createTestInstance.publishSuccess(FakeTaskContext, FakeMagicLinkUrl, FakePublishParams),
        1.second
      )
      val _ = result shouldBe {}
      val _ = captor.value.body shouldBe testMapper.writeValueAsString(ExpectedPublishSuccessPayload)
    }
    "fail" in {
      val _ = mockMqContext.createDefaultPublisher(*[PublisherConfig]) returns mockPublisher
      val _ = mockPublisher.publish(*[Message]) returns Future.exception(new Exception("test exception"))

      val thrown = intercept[PublisherException] {
        Await.result(
          createTestInstance.publishSuccess(FakeTaskContext, FakeMagicLinkUrl, FakePublishParams),
          1.second
        )
      }
      val _ = thrown.getMessage shouldBe "test exception"
    }
  }

  "publish failure message" should {
    "succeed" in {
      val captor = ArgCaptor[Message]
      val _      = mockMqContext.createDefaultPublisher(*[PublisherConfig]) returns mockPublisher
      val _      = mockPublisher.publish(captor) returns Future {}

      val result: Unit = Await.result(
        createTestInstance.publishFailure(FakeTaskContext, FakePublishParams),
        1.second
      )
      val _ = result shouldBe {}
      val _ = captor.value.body shouldBe testMapper.writeValueAsString(ExpectedPublishFailurePayload)
    }
    "fail" in {
      val _ = mockMqContext.createDefaultPublisher(*[PublisherConfig]) returns mockPublisher
      val _ = mockPublisher.publish(*[Message]) returns Future.exception(new Exception("test exception"))

      val thrown = intercept[PublisherException] {
        Await.result(
          createTestInstance.publishFailure(FakeTaskContext, FakePublishParams),
          1.second
        )
      }
      val _ = thrown.getMessage shouldBe "test exception"
    }
  }

}

private object SelfPublisherImplTest {

  private final val FakeMagicLinkUrl = "fake-magic-link-url"

  private final val FakePublishParams = PublishParams(FakeRequestId,
                                                      FakeExternalPatientRef,
                                                      FakeEpisodeRefAlpha,
                                                      FakeLocationId
                                                     )

  private final val ExpectedPublishSuccessPayload = PublishSuccessPayload(FakeRequestId,
                                                                          FakeExternalPatientRef,
                                                                          FakeEpisodeRefAlpha,
                                                                          FakeLocationId,
                                                                          1,
                                                                          FakeMagicLinkUrl
                                                                         )

  private final val ExpectedPublishFailurePayload = PublishFailurePayload(FakeRequestId,
                                                                          FakeExternalPatientRef,
                                                                          FakeEpisodeRefAlpha,
                                                                          FakeLocationId,
                                                                          0
                                                                         )

  private val FakePublisherConfig = PublisherConfig("fake-label",
                                                    isEnabled = true,
                                                    "fake-exchange-name",
                                                    "fake-routing-key",
                                                    isMandatory = true,
                                                    mustWaitForConfirm = true
                                                   )

  private val FakeAppInfo = AppInfo("name", "0.1.0")

  private val FakeParams = EnqueueTestSessionParams(
    requestId = "fake-request-id",
    patientId = "fake-patient-id",
    episodeId = "fake-episode-id",
    patientRef = "fake-patient-id",
    episodeRef = "fake-episode-id",
    locationId = "fake-location-id",
    firstName = "fake-first-name",
    lastName = "fake-last-name",
    birthDate = "fake-dob",
    gender = "fake-gender",
    maybeEmail = Some("fake-email"),
    maybeMobileNumber = Some(123456790)
  )

  private val ExpectedTestSessionMessagePayload = EnqueueTestSessionMessagePayload(
    requestId = "fake-request-id",
    patientRef = "fake-patient-id",
    episodeRef = "fake-episode-id",
    locationId = "fake-location-id",
    firstName = "fake-first-name",
    lastName = "fake-last-name",
    birthDate = "fake-dob",
    gender = "fake-gender",
    source = "fake-source",
    startDate = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES),
    expiryDate = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.MINUTES),
    maybeEmail = Some("fake-email"),
    maybeMobileNumber = Some(123456790)
  )

  private val FakeTaskContext = TaskContext(
    Some(new Date()),
    Some("appId"),
    Some(CreateTestSessionCmd.toString),
    Some("messageId"),
    Some("expiration"),
    Some("correlationId")
  )

  private case class PublishSuccessPayload(requestId: String,
                                           patientRef: String,
                                           episodeRef: String,
                                           locationId: String,
                                           outcome: Int,
                                           magicLinkUrl: String
                                          )

  private case class PublishFailurePayload(requestId: String,
                                           patientRef: String,
                                           episodeRef: String,
                                           locationId: String,
                                           outcome: Int
                                          )

}

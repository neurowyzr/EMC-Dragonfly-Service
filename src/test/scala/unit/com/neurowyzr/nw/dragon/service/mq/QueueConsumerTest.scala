package com.neurowyzr.nw.dragon.service.mq

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.inject.app.TestInjector
import com.twitter.util.{Await, Future}
import com.twitter.util.jackson.ScalaObjectMapper

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeDbfsConfig
import com.neurowyzr.nw.dragon.service.biz.{
  CreateMagicLinkTaskPipeline, CreateTestSessionTaskPipeline, CreateUserTaskPipeline, InvalidateMagicLinkTaskPipeline,
  NotifyClientTaskPipeline, UpdateMagicLinkTaskPipeline, UploadReportTaskPipeline
}
import com.neurowyzr.nw.dragon.service.biz.exceptions.{
  BizException, CreateTestSessionException, ErrorOutcomeException, PublisherException
}
import com.neurowyzr.nw.dragon.service.biz.impl.stages.{
  FakeCreateMagicLinkTaskInput, FakeCreateTestSessionTask, FakeCreateTestSessionTaskInput, FakeCreateUserTaskInput,
  FakeInvalidateMagicLinkTaskInput, FakeNotifyClientTask, FakeNotifyClientTaskInput, FakeUpdateMagicLinkTaskInput,
  FakeUploadReportTask
}
import com.neurowyzr.nw.dragon.service.biz.models.{
  CreateMagicLinkTask, CreateTestSessionTask, CreateUserTask, InvalidateMagicLinkTask, NotifyClientTask, TaskContext,
  UpdateMagicLinkTask, UploadReportTask
}
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus.{DuplicateEpisodeId, DuplicateRequestId}
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.PatientNotFound
import com.neurowyzr.nw.dragon.service.mq.QueueConsumerTest.*
import com.neurowyzr.nw.dragon.service.mq.impl.{CygnusPublisherImpl, SelfPublisherImpl}
import com.neurowyzr.nw.dragon.service.mq.impl.SelfPublisherImpl.PublishParams
import com.neurowyzr.nw.finatra.lib.clients.AlerterHttpClient
import com.neurowyzr.nw.finatra.rabbitmq.lib.{MqConsumer, MqConsumerHandler}
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.{ArbitraryMsgProperties, Message}

import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class QueueConsumerTest
    extends AnyWordSpec with IdiomaticMockito with Matchers with BeforeAndAfterEach with ResetMocksAfterEachTest {

  private val mockConsumer                        = mock[MqConsumer]
  private val mapper                              = new ScalaObjectMapperModule()
  private val testInjector                        = TestInjector(mapper).create()
  private val testMapper                          = testInjector.instance[ScalaObjectMapper]
  private val mockCreateTestSessionTaskPipeline   = mock[CreateTestSessionTaskPipeline]
  private val mockCreateMagicLinkTaskPipeline     = mock[CreateMagicLinkTaskPipeline]
  private val mockCreateUserTaskPipeline          = mock[CreateUserTaskPipeline]
  private val mockUpdateMagicLinkTaskPipeline     = mock[UpdateMagicLinkTaskPipeline]
  private val mockInvalidateMagicLinkTaskPipeline = mock[InvalidateMagicLinkTaskPipeline]
  private val mockNotifyClientTaskPipeline        = mock[NotifyClientTaskPipeline]
  private val mockUploadReportTaskPipeline        = mock[UploadReportTaskPipeline]
  private val mockCygnusPublisher                 = mock[CygnusPublisherImpl]
  private val mockSelfPublisher                   = mock[SelfPublisherImpl]
  private val mockAlerterClient                   = mock[AlerterHttpClient]

  private def createTestInstance =
    new QueueConsumer(
      testMapper,
      mockConsumer,
      mockCreateTestSessionTaskPipeline,
      mockCreateMagicLinkTaskPipeline,
      mockCreateUserTaskPipeline,
      mockUpdateMagicLinkTaskPipeline,
      mockInvalidateMagicLinkTaskPipeline,
      mockNotifyClientTaskPipeline,
      mockUploadReportTaskPipeline,
      mockCygnusPublisher,
      mockSelfPublisher,
      FakeDbfsConfig,
      mockAlerterClient
    )

  override def beforeEach(): Unit = {
    val _ = mockConsumer.consume(*[MqConsumerHandler]) returns Future.Unit
  }

  "QueueConsumer" should {
    "initialisation of the consumer succeeds" in {
      val _ = createTestInstance

      val _ = mockConsumer.consume(*[MqConsumerHandler]) wasCalled once
      val _ = mockConsumer wasNever calledAgain
    }
  }

  "onConsume" when {
    "upload report command is received" should {
      "succeed if pipeline returns Future.True" in {
        val _ = mockUploadReportTaskPipeline.execute(*[UploadReportTask]) returns Future.value(FakeUploadReportTask)

        val testInstance = createTestInstance

        val result: Boolean = Await.result(testInstance.onConsume(FakeUploadReportMessage), 1.second)
        val _               = result shouldBe true
      }
    }

    "notify client command is received" should {
      "succeed if pipeline returns Future.True" in {
        val _ = mockNotifyClientTaskPipeline.execute(*[NotifyClientTask]) returns Future.value(FakeNotifyClientTask)

        val testInstance = createTestInstance

        val result: Boolean = Await.result(testInstance.onConsume(FakeNotifyClientMessage), 1.second)
        val _               = result shouldBe true
      }
    }
    "create test session command is received" should {
      "succeed if pipeline returns Future.True" in {
        val _ =
          mockCreateTestSessionTaskPipeline.execute(*[CreateTestSessionTask]) returns Future.value(
            CreateTestSessionTask(
              TaskContext(),
              FakeCreateTestSessionTaskInput
            )
          )

        val testInstance = createTestInstance

        val result: Boolean = Await.result(testInstance.onConsume(FakeCreateTestSessionMessage), 1.second)
        val _               = result shouldBe true
      }

      "fail when message cannot be parsed due to missing fields" in {
        val invalidMessage = FakeCreateTestSessionMessage.copy(body = InvalidMessage)

        val result = Await.result(createTestInstance.onConsume(invalidMessage), 1.second)

        val _ = result shouldBe false
        val _ = mockCreateTestSessionTaskPipeline wasNever called
      }

      "fail when CreateTestSessionException thrown in pipeline" in {
        mockSelfPublisher.publishFailure(*[TaskContext], *[PublishParams]) returns Future.Unit
        val _ =
          mockCreateTestSessionTaskPipeline.execute(*[CreateTestSessionTask]) returns Future.exception(
            CreateTestSessionException(DuplicateRequestId, "test exception")
          )

        val result = Await.result(createTestInstance.onConsume(FakeCreateTestSessionMessage), 1.second)

        val _ = result shouldBe false
        val _ = mockCreateTestSessionTaskPipeline.execute(*[CreateTestSessionTask]) wasCalled once
        val _ = mockSelfPublisher.publishFailure(*[TaskContext], *[PublishParams]) wasCalled once
      }

      "succeed when DuplicateEpisodeId status thrown in pipeline" in {
        mockSelfPublisher.publishSuccess(*[TaskContext], *[String], *[PublishParams]) returns Future.Unit
        val _ =
          mockCreateTestSessionTaskPipeline.execute(*[CreateTestSessionTask]) returns Future.exception(
            CreateTestSessionException(DuplicateEpisodeId, "test exception")
          )

        val result = Await.result(createTestInstance.onConsume(FakeCreateTestSessionMessage), 1.second)

        val _ = result shouldBe true
        val _ = mockCreateTestSessionTaskPipeline.execute(*[CreateTestSessionTask]) wasCalled once
        val _ = mockSelfPublisher.publishSuccess(*[TaskContext], *[String], *[PublishParams]) wasCalled once
      }

      "fail when publishSuccess throws exception" in {
        mockSelfPublisher.publishSuccess(*[TaskContext], *[String], *[PublishParams]) returns Future.exception(
          PublisherException(FakeCreateTestSessionMessage, "test exception")
        )
        val _ =
          mockCreateTestSessionTaskPipeline.execute(*[CreateTestSessionTask]) returns Future.exception(
            CreateTestSessionException(DuplicateEpisodeId, "test exception")
          )

        val result = Await.result(createTestInstance.onConsume(FakeCreateTestSessionMessage), 1.second)

        val _ = result shouldBe false
        val _ = mockCreateTestSessionTaskPipeline.execute(*[CreateTestSessionTask]) wasCalled once
        val _ = mockSelfPublisher.publishSuccess(*[TaskContext], *[String], *[PublishParams]) wasCalled once
      }

      "fail when publishFailure throws exception" in {
        mockSelfPublisher.publishFailure(*[TaskContext], *[PublishParams]) returns Future.exception(
          PublisherException(FakeCreateTestSessionMessage, "test exception")
        )
        val _ =
          mockCreateTestSessionTaskPipeline.execute(*[CreateTestSessionTask]) returns Future.exception(
            CreateTestSessionException(DuplicateRequestId, "test exception")
          )

        val result = Await.result(createTestInstance.onConsume(FakeCreateTestSessionMessage), 1.second)

        val _ = result shouldBe false
        val _ = mockCreateTestSessionTaskPipeline.execute(*[CreateTestSessionTask]) wasCalled once
        val _ = mockSelfPublisher.publishFailure(*[TaskContext], *[PublishParams]) wasCalled once
      }
    }
    "create magic link command is received" should {
      "succeed if pipeline returns Future.True" in {
        val _ =
          mockCreateMagicLinkTaskPipeline.execute(*[CreateMagicLinkTask]) returns Future.value(
            CreateMagicLinkTask(
              TaskContext(),
              FakeCreateMagicLinkTaskInput
            )
          )

        val result: Unit = Await.result(createTestInstance.onConsume(FakeCreateMagicLinkMessage), 1.second)
        val _            = result shouldBe {}
      }

      "fail when message cannot be parsed due to missing fields" in {
        val invalidMessage = FakeCreateMagicLinkMessage.copy(body = InvalidMessage)

        val _ = Await.result(createTestInstance.onConsume(invalidMessage), 1.second)

        val _ = mockCreateMagicLinkTaskPipeline wasNever called
      }

      "fail when BizException thrown in pipeline" in {
        val _ =
          mockCreateMagicLinkTaskPipeline.execute(*[CreateMagicLinkTask]) returns Future.exception(
            BizException("kaboom!")
          )

        val result: Unit = Await.result(createTestInstance.onConsume(FakeCreateMagicLinkMessage), 1.second)

        val _ = result shouldBe {}
        val _ = mockCreateMagicLinkTaskPipeline.execute(*[CreateMagicLinkTask]) wasCalled once
      }

      "fail when ErrorOutcomeException thrown in pipeline" in {
        val _ =
          mockCreateMagicLinkTaskPipeline.execute(*[CreateMagicLinkTask]) returns Future.exception(
            ErrorOutcomeException(PatientNotFound, "Patient not found!")
          )

        val result: Unit = Await.result(createTestInstance.onConsume(FakeCreateMagicLinkMessage), 1.second)

        val _ = result shouldBe {}
        val _ = mockCreateMagicLinkTaskPipeline.execute(*[CreateMagicLinkTask]) wasCalled once
      }

      "fail when exception thrown in pipeline" in {
        val _ =
          mockCreateMagicLinkTaskPipeline.execute(*[CreateMagicLinkTask]) returns Future.exception(
            new Exception("kaboom")
          )

        val thrown = intercept[Exception] {
          Await.result(createTestInstance.onConsume(FakeCreateMagicLinkMessage), 1.second)
        }

        val _ = thrown.getMessage shouldBe "kaboom"
      }
    }

    "create user command is received" should {
      "succeed if pipeline returns Future.True" in {
        val _ =
          mockCreateUserTaskPipeline.execute(*[CreateUserTask]) returns Future.value(
            CreateUserTask(
              TaskContext(),
              FakeCreateUserTaskInput
            )
          )

        val result: Unit = Await.result(createTestInstance.onConsume(FakeCreateUserMessage), 1.second)
        val _            = result shouldBe {}
      }

      "fail when message cannot be parsed due to missing fields" in {
        val invalidMessage = FakeCreateMagicLinkMessage.copy(body = InvalidMessage)

        val _ = Await.result(createTestInstance.onConsume(invalidMessage), 1.second)

        val _ = mockCreateUserTaskPipeline wasNever called
      }

      "fail when BizException thrown in pipeline" in {
        val _ =
          mockCreateUserTaskPipeline.execute(*[CreateUserTask]) returns Future.exception(
            BizException("kaboom!")
          )

        val result: Unit = Await.result(createTestInstance.onConsume(FakeCreateUserMessage), 1.second)

        val _ = result shouldBe {}
        val _ = mockCreateUserTaskPipeline.execute(*[CreateUserTask]) wasCalled once
      }

      "fail when exception thrown in pipeline" in {
        val _ =
          mockCreateUserTaskPipeline.execute(*[CreateUserTask]) returns Future.exception(
            new Exception("kaboom")
          )

        val thrown = intercept[Exception] {
          Await.result(createTestInstance.onConsume(FakeCreateUserMessage), 1.second)
        }

        val _ = thrown.getMessage shouldBe "kaboom"
      }
    }

    "update magic link command is received" should {
      "succeed if pipeline returns Future.True" in {
        val _ =
          mockUpdateMagicLinkTaskPipeline.execute(*[UpdateMagicLinkTask]) returns Future.value(
            UpdateMagicLinkTask(
              TaskContext(),
              FakeUpdateMagicLinkTaskInput
            )
          )

        val result: Unit = Await.result(createTestInstance.onConsume(FakeUpdateMagicLinkMessage), 1.second)
        val _            = result shouldBe {}
      }

      "fail when message cannot be parsed due to missing fields" in {
        val invalidMessage = FakeUpdateMagicLinkMessage.copy(body = InvalidMessage)

        val _ = Await.result(createTestInstance.onConsume(invalidMessage), 1.second)

        val _ = mockUpdateMagicLinkTaskPipeline wasNever called
      }

      "fail when BizException thrown in pipeline" in {
        val _ =
          mockUpdateMagicLinkTaskPipeline.execute(*[UpdateMagicLinkTask]) returns Future.exception(
            BizException("kaboom!")
          )

        val result: Unit = Await.result(createTestInstance.onConsume(FakeUpdateMagicLinkMessage), 1.second)

        val _ = result shouldBe {}
        val _ = mockUpdateMagicLinkTaskPipeline.execute(*[UpdateMagicLinkTask]) wasCalled once
      }

      "fail when exception thrown in pipeline" in {
        val _ =
          mockUpdateMagicLinkTaskPipeline.execute(*[UpdateMagicLinkTask]) returns Future.exception(
            new Exception("kaboom")
          )

        val thrown = intercept[Exception] {
          Await.result(createTestInstance.onConsume(FakeUpdateMagicLinkMessage), 1.second)
        }

        val _ = thrown.getMessage shouldBe "kaboom"
      }
    }

    "invalidate magic link command is received" should {
      "succeed if pipeline returns Future.True" in {
        val _ =
          mockInvalidateMagicLinkTaskPipeline.execute(*[InvalidateMagicLinkTask]) returns Future.value(
            InvalidateMagicLinkTask(
              TaskContext(),
              FakeInvalidateMagicLinkTaskInput
            )
          )

        val result: Unit = Await.result(createTestInstance.onConsume(FakeInvalidateMagicLinkMessage), 1.second)
        val _            = result shouldBe {}
      }

      "fail when message cannot be parsed due to missing fields" in {
        val invalidMessage = FakeInvalidateMagicLinkMessage.copy(body = InvalidMessage)

        val _ = Await.result(createTestInstance.onConsume(invalidMessage), 1.second)

        val _ = mockInvalidateMagicLinkTaskPipeline wasNever called
      }

      "fail when BizException thrown in pipeline" in {
        val _ =
          mockInvalidateMagicLinkTaskPipeline.execute(*[InvalidateMagicLinkTask]) returns Future.exception(
            BizException("kaboom!")
          )

        val result: Unit = Await.result(createTestInstance.onConsume(FakeInvalidateMagicLinkMessage), 1.second)

        val _ = result shouldBe {}
        val _ = mockInvalidateMagicLinkTaskPipeline.execute(*[InvalidateMagicLinkTask]) wasCalled once
      }

      "fail when exception thrown in pipeline" in {
        val _ =
          mockInvalidateMagicLinkTaskPipeline.execute(*[InvalidateMagicLinkTask]) returns Future.exception(
            new Exception("kaboom")
          )

        val thrown = intercept[Exception] {
          Await.result(createTestInstance.onConsume(FakeInvalidateMagicLinkMessage), 1.second)
        }

        val _ = thrown.getMessage shouldBe "kaboom"
      }
    }

    "other commands are received" should {
      "fail when message cannot be parsed due to missing type" in {
        val result: Unit = Await.result(createTestInstance.onConsume(FakeInvalidMessage), 1.second)

        val _ = result shouldBe {}
        val _ = mockCreateMagicLinkTaskPipeline wasNever called
        val _ = mockCreateUserTaskPipeline wasNever called
      }

      "fail when message type was not handled" in {
        val result: Unit = Await.result(createTestInstance.onConsume(FakeInvalidMessageType))

        val _ = result shouldBe {}
        val _ = mockCreateMagicLinkTaskPipeline wasNever called
        val _ = mockCreateUserTaskPipeline wasNever called
      }
    }

  }

}

private object QueueConsumerTest {

  val InvalidMessage = """{ "fake": "fake" }"""

  val FakeUploadReportTaskMsgProperties = ArbitraryMsgProperties.apply(
    "appId",
    UploadCmd.toString,
    "message-id",
    Some("expiration")
  )

  val FakeNotifyClientTaskMsgProperties = ArbitraryMsgProperties.apply(
    "unit test",
    NotifyClientTaskCmd.toString,
    "fake-id",
    None
  )

  val FakeCreateTestSessionMsgProperties: ArbitraryMsgProperties = ArbitraryMsgProperties.apply(
    "unit test",
    CreateTestSessionCmd.toString,
    "fake-id",
    None
  )

  val FakeCreateMagicLinkMsgProperties: ArbitraryMsgProperties = ArbitraryMsgProperties.apply(
    "unit test",
    CreateMagicLinkCmd.toString,
    "fake-id",
    None
  )

  val FakeCreateUserMsgProperties: ArbitraryMsgProperties = ArbitraryMsgProperties.apply("unit test",
                                                                                         CreateUserCmd.toString,
                                                                                         "fake-id",
                                                                                         None
                                                                                        )

  val FakeUpdateMagicLinkMsgProperties: ArbitraryMsgProperties = ArbitraryMsgProperties.apply(
    "unit test",
    UpdateMagicLinkCmd.toString,
    "fake-id",
    None
  )

  val FakeInvalidateMagicLinkMsgProperties: ArbitraryMsgProperties = ArbitraryMsgProperties.apply(
    "unit test",
    InvalidateMagicLinkCmd.toString,
    "fake-id",
    None
  )

  val FakeMsgPropertiesWithInvalidMsgType: ArbitraryMsgProperties = ArbitraryMsgProperties.apply("unit test",
                                                                                                 "fake-type",
                                                                                                 "fake-id",
                                                                                                 None
                                                                                                )

  final val FakeUploadReportMessage = Message(
    """
      |{
      |  "episode_id": 12345,
      |  "user_batch_code": "fake-user-batch-code",
      |  "s3_urls": [
      |     "fake-url"
      |   ]
      |}
      |""".stripMargin,
    Some(FakeUploadReportTaskMsgProperties)
  )

  final val FakeNotifyClientMessage = Message(
    """
      |{
      |  "request_id": "fake-request-id",
      |  "patient_ref": "fake-uid",
      |  "episode_ref": "fake-ahc",
      |  "location_id": "fake-location-id",
      |  "outcome": 1,
      |  "magic_link": "fake-magic-link"
      |}
      |""".stripMargin,
    Some(FakeNotifyClientTaskMsgProperties)
  )

  final val FakeCreateTestSessionMessage = Message(
    """
      |{
      |  "request_id": "fake-request-id",
      |  "patient_ref": "fake-uid",
      |  "episode_ref": "fake-ahc",
      |  "location_id": "fake-location-id",
      |  "first_name": "fake-first-name",
      |  "last_name": "fake-last-name",
      |  "email": "fake-email",
      |  "mobile": "1234567890",
      |  "birth_date": "1990-12-31",
      |  "gender": "MALE",
      |  "source": "fake-source",
      |  "start_date": "2024-08-01T12:00:00",
      |  "expiry_date": "2024-12-31T12:00:00"
      |}
      |""".stripMargin,
    Some(FakeCreateTestSessionMsgProperties)
  )

  final val FakeCreateMagicLinkMessage = Message(
    """
      |{
      |  "source": "fake-source",
      |  "user_batch_code": "fake-code",
      |  "patient_id": "fake-patient-id",
      |  "test_id": "fake-test-id",
      |  "start_date": "2023-07-01T12:00:00",
      |  "expiry_date": "2023-07-01T12:00:00"
      |}
      |""".stripMargin,
    Some(FakeCreateMagicLinkMsgProperties)
  )

  final val FakeCreateUserMessage = Message(
    """
      |{
      |  "source": "fake-source",
      |  "patient_id": "fake-patient-id"
      |}
      |""".stripMargin,
    Some(FakeCreateUserMsgProperties)
  )

  final val FakeUpdateMagicLinkMessage = Message(
    """
      |{
      |  "test_id": "fake-test-id",
      |  "expiry_date": "2023-07-01T12:00:00"
      |}
      |""".stripMargin,
    Some(FakeUpdateMagicLinkMsgProperties)
  )

  final val FakeInvalidateMagicLinkMessage = Message(
    """
      |{
      |  "test_id": "fake-test-id"
      |}
      |""".stripMargin,
    Some(FakeInvalidateMagicLinkMsgProperties)
  )

  final val FakeInvalidMessage = Message("invalid")

  final val FakeInvalidMessageType = Message(
    """
      |{
      |  "fake": "fake"
      |}
      |""".stripMargin,
    Some(FakeMsgPropertiesWithInvalidMsgType)
  )

}

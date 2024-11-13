package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.exceptions.{CreateTestSessionException, PublisherException}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.NotifyCygnusStage
import com.neurowyzr.nw.dragon.service.biz.impl.stages.FakeCreateTestSessionTask
import com.neurowyzr.nw.dragon.service.biz.models.{CreateTestSessionTask, CreateUserTask, Outcome, Outcomes, TaskContext}
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.{PatientNotFound, PatientRefExist}
import com.neurowyzr.nw.dragon.service.mq.impl.{CygnusPublisherImpl, SelfPublisherImpl}
import com.neurowyzr.nw.dragon.service.mq.impl.SelfPublisherImpl.PublishParams
import com.neurowyzr.nw.finatra.lib.clients.AlerterHttpClient
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.Message

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PublishFeedbackStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  private val mockPublisher     = mock[SelfPublisherImpl]
  private val mockAlerterClient = mock[AlerterHttpClient]
  private val testInstance      = new PublishFeedbackStage(mockPublisher, mockAlerterClient)

  "publish feedback" should {
    "succeed" in {
      val task = FakeCreateTestSessionTask
        .modify(_.out.maybeOutcome)
        .setTo(Some(Outcomes.Success))
        .modify(_.out.maybeMagicLinkUrl)
        .setTo(Some("fake-magic-link-url"))
      val urlCaptor = ArgCaptor[String]
      val _         = mockPublisher.publishSuccess(*[TaskContext], urlCaptor, *[PublishParams]) returns Future {}

      val result = Await.result(testInstance.execute(task))

      val _ = urlCaptor.value shouldBe "fake-magic-link-url"
      result shouldBe task
    }

    "fail when outcome is unexpected" in {
      val task = FakeCreateTestSessionTask.modify(_.out.maybeMagicLinkUrl).setTo(Some("fake-magic-link-url"))

      val thrown = intercept[CreateTestSessionException] {
        Await.result(testInstance.execute(task))
      }

      thrown.getMessage shouldBe "Unexpected outcome: None and request id: fake-request-id."
    }

    "swallow error when exception is thrown when publishing message" in {
      val task = FakeCreateTestSessionTask
        .modify(_.out.maybeOutcome)
        .setTo(Some(Outcomes.Success))
        .modify(_.out.maybeMagicLinkUrl)
        .setTo(Some("fake-magic-link-url"))

      mockPublisher.publishSuccess(*[TaskContext], *[String], *[PublishParams]) returns Future.exception(
        PublisherException(Message("fake-body"), "test exception")
      )

      val result = Await.result(testInstance.execute(task))

      result shouldBe task
    }
  }

}

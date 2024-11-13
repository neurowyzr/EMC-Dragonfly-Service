package com.neurowyzr.nw.dragon.service.biz.impl.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeMessageId, FakeMessageType}
import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.DuplicateEventFilterStage
import com.neurowyzr.nw.dragon.service.biz.models.{CreateMagicLinkTask, CygnusEvent}
import com.neurowyzr.nw.dragon.service.data.CygnusEventRepository

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DuplicateEventFilterStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: CygnusEventRepository = mock[CygnusEventRepository]
  private val testInstance            = new DuplicateEventFilterStage[CreateMagicLinkTask](mockRepo)

  "message id is found" should {
    "throw exception if there is duplicate" in {
      val fakeCygnusEvent = CygnusEvent(FakeMessageType, FakeMessageId)
      val _ =
        mockRepo.getCygnusEventByMessageTypeAndMessageId(*[String], *[String]) returns Future.value(Some(fakeCygnusEvent))

      val thrown = intercept[BizException] {
        Await.result(testInstance.execute(FakeCreateMagicLinkTask))
      }
      thrown.getMessage shouldBe s"Duplicate event: '${FakeCreateMagicLinkTask.ctx.maybeMessageId.get}'"
    }

    "return task and create event if there is no duplicate" in {
      val fakeCygnusEvent = CygnusEvent(FakeMessageType, FakeMessageId)
      val _ = mockRepo.getCygnusEventByMessageTypeAndMessageId(*[String], *[String]) returns Future.value(None)
      val _ = mockRepo.createCygnusEvent(*[CygnusEvent]) returns Future.value(fakeCygnusEvent)

      val result = Await.result(testInstance.execute(FakeCreateMagicLinkTask))

      result shouldBe FakeCreateMagicLinkTask
    }
  }

  "missing message id" should {
    "throw exception" in {
      val invalidTask = FakeCreateMagicLinkTask.modify(_.ctx.maybeMessageId).setTo(None)

      val thrown = intercept[BizException] {
        Await.result(testInstance.execute(invalidTask))
      }
      thrown.getMessage shouldBe s"Missing messageId and/or messageType in task: '${invalidTask.toString}'"

    }
  }

}

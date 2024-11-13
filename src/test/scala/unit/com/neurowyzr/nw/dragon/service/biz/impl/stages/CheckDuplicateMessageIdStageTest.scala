package com.neurowyzr.nw.dragon.service.biz.impl.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeEpisodeAlpha
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.CheckDuplicateMessageIdStage
import com.neurowyzr.nw.dragon.service.biz.impl.stages.ValidateInputStageTest.FakeTaskContextPopulated
import com.neurowyzr.nw.dragon.service.data.EpisodeRepository

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CheckDuplicateMessageIdStageTest
    extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: EpisodeRepository = mock[EpisodeRepository]
  private val testInstance        = new CheckDuplicateMessageIdStage(mockRepo)

  "pass if message id is not found" in {
    val _         = mockRepo.getEpisodeByMessageId(*[String]) returns Future.value(None)
    val validTask = FakeCreateMagicLinkTask.copy(ctx = FakeTaskContextPopulated)

    val result = Await.result(testInstance.execute(validTask))
    result shouldBe validTask
  }

  "fail if message id is found" in {
    val _         = mockRepo.getEpisodeByMessageId(*[String]) returns Future.value(Some(FakeEpisodeAlpha))
    val validTask = FakeCreateMagicLinkTask.copy(ctx = FakeTaskContextPopulated)

    val thrown = intercept[Exception] {
      Await.result(testInstance.execute(validTask))
    }
    thrown.getMessage shouldBe s"Message id: ${validTask.ctx.messageId} exists. This is a duplicate message."
  }

}

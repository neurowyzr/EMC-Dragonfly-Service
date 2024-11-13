package com.neurowyzr.nw.dragon.service.biz.impl.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.exceptions.ErrorOutcomeException
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeUserBatch
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.VerifyUserBatchCodeExistStage
import com.neurowyzr.nw.dragon.service.biz.impl.stages.ValidateInputStageTest.FakeTaskContextPopulated
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.UserBatchCodeMissing
import com.neurowyzr.nw.dragon.service.data.UserBatchRepository

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VerifyUserBatchCodeExistStageTest
    extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  private val mockRepo     = mock[UserBatchRepository]
  private val testInstance = new VerifyUserBatchCodeExistStage(mockRepo)

  "pass if user batch is found" in {
    val _         = mockRepo.getUserBatchByCode(*[String]) returns Future.value(Some(FakeUserBatch))
    val validTask = FakeCreateMagicLinkTask.copy(ctx = FakeTaskContextPopulated)

    val result = Await.result(testInstance.execute(validTask))

    val validTaskMod = validTask.modify(_.out.userBatchId).setTo(Some(FakeUserBatch.id))
    val _            = result shouldBe validTaskMod
  }

  "outcome error if user batch is not found" in {
    val _         = mockRepo.getUserBatchByCode(*[String]) returns Future.value(None)
    val validTask = FakeCreateMagicLinkTask.copy(ctx = FakeTaskContextPopulated)

    val thrown = intercept[ErrorOutcomeException] {
      Await.result(testInstance.execute(validTask))
    }

    val _ = thrown.outcome shouldBe UserBatchCodeMissing
  }

}

package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeUserBatchCode
import com.neurowyzr.nw.dragon.service.biz.exceptions.CreateTestSessionException
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.{FakeEpisodeAlpha, FakeUserBatch}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages.VerifyUserBatchCodeStageTest.{
  FakeOutputPopulated, FakeTask
}
import com.neurowyzr.nw.dragon.service.biz.impl.stages.{FakeCreateTestSessionTask, FakeCreateTestSessionTaskInput}
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionTaskOutput
import com.neurowyzr.nw.dragon.service.data.{EpisodeRepository, UserBatchRepository}

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VerifyUserBatchCodeStageTest
    extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: UserBatchRepository = mock[UserBatchRepository]
  private val testInstance          = new VerifyUserBatchCodeStage(mockRepo)

  "error if user batch code is empty" in {
    val thrown = intercept[CreateTestSessionException] {
      Await.result(testInstance.execute(FakeCreateTestSessionTask))
    }

    val _ = thrown.getMessage shouldBe "User batch code is empty for request id fake-request-id."

  }

  "error if user batch code is not found" in {
    val _ = mockRepo.getUserBatchByCode(*[String]) returns Future.value(None)

    val thrown = intercept[CreateTestSessionException] {
      Await.result(testInstance.execute(FakeTask))
    }

    val _ =
      thrown.getMessage shouldBe "User batch not found for user batch code: fakeco and request id: fake-request-id."

  }

  "continue if user batch code is found" in {
    val _ = mockRepo.getUserBatchByCode(*[String]) returns Future.value(Some(FakeUserBatch))

    val result = Await.result(testInstance.execute(FakeTask))

    val validTaskMod = FakeTask.modify(_.out.maybeUserBatchId).setTo(Some(FakeUserBatch.id))

    val _ = result shouldBe validTaskMod
    val _ = mockRepo.getUserBatchByCode(*[String]) wasCalled once
    val _ = mockRepo wasNever calledAgain
  }

}

object VerifyUserBatchCodeStageTest {
  final val FakeOutputPopulated = CreateTestSessionTaskOutput(None, None, Some(FakeUserBatchCode), None, None, None)
  final val FakeTask            = FakeCreateTestSessionTask.copy(out = FakeOutputPopulated)
}

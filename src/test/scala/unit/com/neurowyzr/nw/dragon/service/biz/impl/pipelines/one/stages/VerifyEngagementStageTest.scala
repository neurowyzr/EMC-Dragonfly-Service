package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeUserBatchCode, FakeUserBatchId}
import com.neurowyzr.nw.dragon.service.biz.exceptions.CreateTestSessionException
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.{FakeEngagement, FakeUserBatchLookup}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages.VerifyEngagementStageTest.FakeTask
import com.neurowyzr.nw.dragon.service.biz.impl.stages.FakeCreateTestSessionTask
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionTaskOutput
import com.neurowyzr.nw.dragon.service.data.{EngagementRepository, UserBatchLookupRepository}

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VerifyEngagementStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: EngagementRepository = mock[EngagementRepository]
  private val testInstance           = new VerifyEngagementStage(mockRepo)

  "outcome error if engagement is not found" in {
    val _ = mockRepo.getEngagementByUserBatchCode(*[String]) returns Future.value(None)

    val thrown = intercept[CreateTestSessionException] {
      Await.result(testInstance.execute(FakeTask))
    }

    val _ =
      thrown.getMessage shouldBe "Engagement not found for user batch code: fakeco and request id: fake-request-id."

  }

  "pass if location id is found" in {
    val _ = mockRepo.getEngagementByUserBatchCode(*[String]) returns Future.value(Some(FakeEngagement))

    val result = Await.result(testInstance.execute(FakeTask))

    val validTaskMod = FakeTask.modify(_.out.maybeEngagementId).setTo(Some(FakeEngagement.id))

    val _ = result shouldBe validTaskMod
    val _ = mockRepo.getEngagementByUserBatchCode(*[String]) wasCalled once
    val _ = mockRepo wasNever calledAgain
  }

}

object VerifyEngagementStageTest {

  final val FakeOutputPopulated = CreateTestSessionTaskOutput(
    None,
    Some(FakeUserBatchId),
    Some(FakeUserBatchCode),
    None,
    None,
    None
  )

  final val FakeTask = FakeCreateTestSessionTask.copy(out = FakeOutputPopulated)
}

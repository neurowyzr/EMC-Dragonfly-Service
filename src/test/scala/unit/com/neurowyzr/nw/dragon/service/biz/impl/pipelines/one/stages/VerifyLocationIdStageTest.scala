package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.exceptions.CreateTestSessionException
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.{FakeEpisodeAlpha, FakeUserBatchLookup}
import com.neurowyzr.nw.dragon.service.biz.impl.stages.FakeCreateTestSessionTask
import com.neurowyzr.nw.dragon.service.data.{EpisodeRepository, UserBatchLookupRepository}

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VerifyLocationIdStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: UserBatchLookupRepository = mock[UserBatchLookupRepository]
  private val testInstance                = new VerifyLocationIdStage(mockRepo)

  "outcome error if location id not found" in {
    val _ = mockRepo.getUserBatchLookupByKey(*[String]) returns Future.value(None)

    val thrown = intercept[CreateTestSessionException] {
      Await.result(testInstance.execute(FakeCreateTestSessionTask))
    }

    val _ =
      thrown.getMessage shouldBe "User batch code not found for location id: fake-location-id and request id: fake-request-id."

  }

  "pass if location id is found" in {
    val _ = mockRepo.getUserBatchLookupByKey(*[String]) returns Future.value(Some(FakeUserBatchLookup))

    val result = Await.result(testInstance.execute(FakeCreateTestSessionTask))

    val validTaskMod = FakeCreateTestSessionTask.modify(_.out.maybeUserBatchCode).setTo(Some(FakeUserBatchLookup.value))

    val _ = result shouldBe validTaskMod
    val _ = mockRepo.getUserBatchLookupByKey(*[String]) wasCalled once
    val _ = mockRepo wasNever calledAgain
  }

}

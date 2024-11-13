package com.neurowyzr.nw.dragon.service.biz.impl.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeNewUserId, FakeUserBatchId}
import com.neurowyzr.nw.dragon.service.biz.exceptions.ErrorOutcomeException
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.{FakeEngagement, FakeNewEpisode, FakeTestSession}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.CreateMagicLinkStage
import com.neurowyzr.nw.dragon.service.biz.impl.stages.CreateMagicLinkStageTest.FakeTask
import com.neurowyzr.nw.dragon.service.biz.models.{Episode, Outcomes, TestSession}
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.EngagementNotFound
import com.neurowyzr.nw.dragon.service.data.{EngagementRepository, EpisodeRepository}

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CreateMagicLinkStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {
  val mockEpisodeRepo: EpisodeRepository       = mock[EpisodeRepository]
  val mockEngagementRepo: EngagementRepository = mock[EngagementRepository]
  private val testInstance                     = new CreateMagicLinkStage(mockEpisodeRepo, mockEngagementRepo)

  "returns task" in {
    val _ = mockEngagementRepo.getEngagementByUserBatchCode(*[String]) returns Future.value(Some(FakeEngagement))
    val _ =
      mockEpisodeRepo.insertEpisodeAndTestSession(*[Episode], *[TestSession]) returns Future.value(
        (FakeNewEpisode, FakeTestSession)
      )

    val result = Await.result(testInstance.execute(FakeTask))
    val _      = result shouldBe FakeTask.modify(_.out.maybeOutcome).setTo(Some(Outcomes.Success))
  }

  "outcome error if engagement doesn't exist" in {
    val _ = mockEngagementRepo.getEngagementByUserBatchCode(*[String]) returns Future.value(None)

    val thrown = intercept[ErrorOutcomeException] {
      Await.result(testInstance.execute(FakeTask))
    }

    val _ = thrown.outcome shouldBe EngagementNotFound
  }

}

object CreateMagicLinkStageTest {

  final val FakeTask = FakeCreateMagicLinkTask
    .modify(_.out.userId)
    .setTo(Some(FakeNewUserId))
    .modify(_.out.userBatchId)
    .setTo(Some(FakeUserBatchId))

}

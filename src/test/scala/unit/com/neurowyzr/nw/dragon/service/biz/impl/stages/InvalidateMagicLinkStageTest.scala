package com.neurowyzr.nw.dragon.service.biz.impl.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.{FakeEpisodeAlpha, FakeEpisodeBravo}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.InvalidateMagicLinkStage
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.EpisodeNotFound
import com.neurowyzr.nw.dragon.service.data.EpisodeRepository

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InvalidateMagicLinkStageTest
    extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: EpisodeRepository = mock[EpisodeRepository]
  private val testInstance        = new InvalidateMagicLinkStage(mockRepo)

  "execute" should {
    "invalidate the episode and set outcome to Success for an existing episode" in {
      val currentTask = FakeInvalidateMagicLinkTask
      val _           = mockRepo.getEpisodeByTestId(*[String]) returns Future.value(Some(FakeEpisodeAlpha))
      val _           = mockRepo.invalidateEpisode(*[Long]) returns Future.value(FakeEpisodeBravo.id)

      val result = Await.result(testInstance.execute(currentTask))

      val _ = result shouldBe FakeInvalidateMagicLinkTask.modify(_.out.maybeOutcome).setTo(Some(Outcomes.Success))
      val _ = mockRepo.getEpisodeByTestId(*[String]) wasCalled once
      val _ = mockRepo.invalidateEpisode(*[Long]) wasCalled once
      val _ = mockRepo wasNever calledAgain

    }

    "set outcome to Error for a non-existing episode" in {
      val currentTask = FakeInvalidateMagicLinkTask
      val _           = mockRepo.getEpisodeByTestId(*[String]) returns Future.None

      val result = Await.result(testInstance.execute(currentTask))

      val _ = result shouldBe FakeInvalidateMagicLinkTask.modify(_.out.maybeOutcome).setTo(Some(EpisodeNotFound))
      val _ = mockRepo.getEpisodeByTestId(*[String]) wasCalled once
      val _ = mockRepo wasNever calledAgain
    }
  }

}

package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.exceptions.CreateTestSessionException
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeEpisodeAlpha
import com.neurowyzr.nw.dragon.service.biz.impl.stages.FakeCreateTestSessionTask
import com.neurowyzr.nw.dragon.service.data.EpisodeRepository

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VerifyUniqueEpisodeIdStageTest
    extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: EpisodeRepository = mock[EpisodeRepository]
  private val testInstance        = new VerifyUniqueEpisodeIdStage(mockRepo)

  "error if episode id found" in {
    val _ = mockRepo.getEpisodeByEpisodeRefAndSource(*[String], *[String]) returns Future.value(Some(FakeEpisodeAlpha))

    val thrown = intercept[CreateTestSessionException] {
      Await.result(testInstance.execute(FakeCreateTestSessionTask))
    }

    val _ =
      thrown.getMessage shouldBe "Episode ref: fake-episode-id already exists, returning previously generated magic link with request id: fake-request-id."

  }

  "continue if episode id is not found" in {
    val _ = mockRepo.getEpisodeByEpisodeRefAndSource(*[String], *[String]) returns Future.value(None)

    val result = Await.result(testInstance.execute(FakeCreateTestSessionTask))

    val _ = result shouldBe FakeCreateTestSessionTask
    val _ = mockRepo.getEpisodeByEpisodeRefAndSource(*[String], *[String]) wasCalled once
    val _ = mockRepo wasNever calledAgain
  }

}

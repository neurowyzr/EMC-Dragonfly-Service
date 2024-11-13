package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeUserBatchCode
import com.neurowyzr.nw.dragon.service.biz.exceptions.{CreateTestSessionException, UploadReportException}
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.{FakeEpisodeAlpha, FakeUserBatch}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages.VerifyUserBatchCodeStageTest.FakeTask
import com.neurowyzr.nw.dragon.service.biz.impl.stages.{FakeCreateTestSessionTask, FakeUploadReportTask}
import com.neurowyzr.nw.dragon.service.biz.models.{CreateTestSessionTaskOutput, Episode, UploadReportTaskOutput}
import com.neurowyzr.nw.dragon.service.data.{EpisodeRepository, UserBatchRepository}

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VerifyEpisodeStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: EpisodeRepository = mock[EpisodeRepository]
  private val testInstance        = new VerifyEpisodeStage(mockRepo)

  "fail if episode id is not valid" in {
    val _ = mockRepo.getEpisodeById(*[Long]) returns Future.value(None)

    val thrown = intercept[UploadReportException] {
      Await.result(testInstance.execute(FakeUploadReportTask))
    }

    val _ = thrown.getMessage shouldBe "Episode does not exist for episode id: 12345"

  }

  "fail if message id does not exist for episode" in {
    val fakeEpisode = FakeEpisodeAlpha.copy(maybeMessageId = None)
    val _           = mockRepo.getEpisodeById(*[Long]) returns Future.value(Some(fakeEpisode))

    val thrown = intercept[UploadReportException] {
      Await.result(testInstance.execute(FakeUploadReportTask))
    }

    val _ = thrown.getMessage shouldBe "Message id does not exist for episode with episode id: 12345"
  }

  "succeed if episode exists for a valid episode" in {
    val _ = mockRepo.getEpisodeById(*[Long]) returns Future.value(Some(FakeEpisodeAlpha))

    val result = Await.result(testInstance.execute(FakeUploadReportTask))

    val expectedOutput =
      new UploadReportTaskOutput(
        maybeRequestId = FakeEpisodeAlpha.maybeMessageId,
        maybeEpisodeRef = Some(FakeEpisodeAlpha.episodeRef),
        maybeUserId = Some(FakeEpisodeAlpha.userId),
        maybePatientRef = None,
        maybeLocationId = None,
        maybeReport = None,
        maybeOutcome = None
      )

    val expectedResult = FakeUploadReportTask.copy(out = expectedOutput)

    val _ = result shouldBe expectedResult
  }

}

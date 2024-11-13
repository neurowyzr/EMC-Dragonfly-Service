package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeUserBatchCode, FakeUserIdAlpha}
import com.neurowyzr.nw.dragon.service.biz.exceptions.{CreateTestSessionException, UploadReportException}
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.{FakeEpisodeAlpha, FakeUser, FakeUserBatch}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages.VerifyUserBatchCodeStageTest.FakeTask
import com.neurowyzr.nw.dragon.service.biz.impl.stages.{FakeCreateTestSessionTask, FakeUploadReportTask}
import com.neurowyzr.nw.dragon.service.biz.models.{CreateTestSessionTaskOutput, Episode, UploadReportTaskOutput}
import com.neurowyzr.nw.dragon.service.data.{EpisodeRepository, UserBatchRepository, UserRepository}

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VerifyPatientStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: UserRepository = mock[UserRepository]
  private val testInstance     = new VerifyPatientStage(mockRepo)

  "fail if user id is empty" in {
    val thrown = intercept[UploadReportException] {
      Await.result(testInstance.execute(FakeUploadReportTask))
    }

    val _ = thrown.getMessage shouldBe "Unexpected error: empty user id"
  }

  "fail if user id is not valid" in {
    val _ = mockRepo.getUserById(*[Long]) returns Future.value(None)

    val fakeOutput = new UploadReportTaskOutput(None, None, Some(FakeUserIdAlpha), None, None, None, None)
    val fakeTask   = FakeUploadReportTask.copy(out = fakeOutput)
    val thrown = intercept[UploadReportException] {
      Await.result(testInstance.execute(fakeTask))
    }

    val _ = thrown.getMessage shouldBe "User does not exist for user id: 555"

  }

  "fail if patient ref does not exist for user" in {
    val _ =
      mockRepo.getUserById(*[Long]) returns
        Future.value(Some(FakeUser.copy(maybeExternalPatientRef = None)))

    val fakeOutput = new UploadReportTaskOutput(None, None, Some(FakeUserIdAlpha), None, None, None, None)
    val fakeTask   = FakeUploadReportTask.copy(out = fakeOutput)
    val thrown = intercept[UploadReportException] {
      Await.result(testInstance.execute(fakeTask))
    }

    val _ = thrown.getMessage shouldBe "Patient ref does not exist for user id: 555"

  }

  "succeed if user id is valid" in {
    val _ = mockRepo.getUserById(*[Long]) returns Future.value(Some(FakeUser))

    val fakeOutput = new UploadReportTaskOutput(None, None, Some(FakeUserIdAlpha), None, None, None, None)
    val fakeTask   = FakeUploadReportTask.copy(out = fakeOutput)
    val result     = Await.result(testInstance.execute(fakeTask))

    val expectedOutput = fakeOutput.copy(maybePatientRef = FakeUser.maybeExternalPatientRef)

    val _ = result shouldBe fakeTask.copy(out = expectedOutput)

  }

}

package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.exceptions.UploadReportException
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeUserBatchLookup
import com.neurowyzr.nw.dragon.service.biz.impl.stages.FakeUploadReportTask
import com.neurowyzr.nw.dragon.service.biz.models.UploadReportTaskOutput
import com.neurowyzr.nw.dragon.service.data.UserBatchLookupRepository

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VerifyUserBatchStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: UserBatchLookupRepository = mock[UserBatchLookupRepository]
  private val testInstance                = new VerifyUserBatchStage(mockRepo)

  "fail if user batch code is invalid" in {
    val _ = mockRepo.getUserBatchLookupByCode(*[String]) returns Future.value(None)

    val thrown = intercept[UploadReportException] {
      Await.result(testInstance.execute(FakeUploadReportTask))
    }

    val _ = thrown.getMessage shouldBe "Location id does not exist for user batch code: fake-user-batch-code"

  }

  "succeed if user batch code is valid" in {
    val _ = mockRepo.getUserBatchLookupByCode(*[String]) returns Future.value(Some(FakeUserBatchLookup))

    val result = Await.result(testInstance.execute(FakeUploadReportTask))

    val expectedOutput = new UploadReportTaskOutput(None, None, None, None, Some(FakeUserBatchLookup.key), None, None)

    val _ = result shouldBe FakeUploadReportTask.copy(out = expectedOutput)

  }

}

package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.{
  FakeAttachmentOutput, FakeEpisodeRefAlpha, FakeExternalPatientRef, FakeFile, FakeFileName, FakeLocationId,
  FakeRequestId
}
import com.neurowyzr.nw.dragon.service.biz.exceptions.UploadReportException
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages.DownloadReportStage.selectReportFromSeq
import com.neurowyzr.nw.dragon.service.biz.impl.stages.FakeUploadReportTask
import com.neurowyzr.nw.dragon.service.biz.models.{AttachmentOutput, UploadReportTaskOutput}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.CreateTestSessionArgs
import com.neurowyzr.nw.dragon.service.clients.{AwsS3Client, CustomerHttpClient}

import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UploadReportStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockClient: CustomerHttpClient = mock[CustomerHttpClient]
  private val testInstance           = new UploadReportStage(mockClient)

  "fail if report is empty" in {
    val _ = mockClient.uploadReport(*[CreateTestSessionArgs], *[Array[Byte]]) returns Future.Unit

    val fakeOutput = UploadReportTaskOutput().copy(
      maybeRequestId = Some(FakeRequestId),
      maybeEpisodeRef = Some(FakeEpisodeRefAlpha),
      maybePatientRef = Some(FakeExternalPatientRef),
      maybeLocationId = Some(FakeLocationId)
    )
    val fakeTask = FakeUploadReportTask.copy(out = fakeOutput)
    val thrown = intercept[UploadReportException] {
      Await.result(testInstance.execute(fakeTask))
    }

    val _ = thrown.getMessage shouldBe "Unexpected error: report is empty"
  }

  "fail if requestId, episodeRef, patientRef or locationId is empty" in {
    val _ = mockClient.uploadReport(*[CreateTestSessionArgs], *[Array[Byte]]) returns Future.Unit

    val fakeOutput = UploadReportTaskOutput().copy(
      maybeRequestId = Some(FakeRequestId),
      maybePatientRef = Some(FakeExternalPatientRef),
      maybeLocationId = Some(FakeLocationId),
      maybeReport = Some(FakeAttachmentOutput)
    )
    val fakeTask = FakeUploadReportTask.copy(out = fakeOutput)
    val thrown = intercept[UploadReportException] {
      Await.result(testInstance.execute(fakeTask))
    }

    val _ = thrown.getMessage shouldBe "Unexpected error: requestId, episodeRef, patientRef or locationId is empty"
  }

  "succeed if report exists" in {
    val _ = mockClient.uploadReport(*[CreateTestSessionArgs], *[Array[Byte]]) returns Future.Unit

    val fakeOutput = UploadReportTaskOutput().copy(
      maybeRequestId = Some(FakeRequestId),
      maybeEpisodeRef = Some(FakeEpisodeRefAlpha),
      maybePatientRef = Some(FakeExternalPatientRef),
      maybeLocationId = Some(FakeLocationId),
      maybeReport = Some(FakeAttachmentOutput)
    )
    val fakeTask = FakeUploadReportTask.copy(out = fakeOutput)
    val result   = Await.result(testInstance.execute(fakeTask))

    val _ = result shouldBe fakeTask
  }

}

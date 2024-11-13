package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeFile, FakeFileName}
import com.neurowyzr.nw.dragon.service.biz.exceptions.UploadReportException
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeEpisodeAlpha
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages.DownloadReportStage.selectReportFromSeq
import com.neurowyzr.nw.dragon.service.biz.impl.stages.FakeUploadReportTask
import com.neurowyzr.nw.dragon.service.biz.models.{AttachmentOutput, UploadReportTaskOutput}
import com.neurowyzr.nw.dragon.service.clients.AwsS3Client
import com.neurowyzr.nw.dragon.service.data.EpisodeRepository

import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DownloadReportStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockClient: AwsS3Client = mock[AwsS3Client]
  private val testInstance    = new DownloadReportStage(mockClient)

  "succeed if episode exists for a valid episode" in {
    val urlCaptor = ArgCaptor[String]
    val _         = mockClient.getFileName(*[String]) returns FakeFileName
    val _         = mockClient.fetchFileFromS3(urlCaptor) returns Future(FakeFile)

    val result = Await.result(testInstance.execute(FakeUploadReportTask))

    val expectedOutput = UploadReportTaskOutput()
      .copy(maybeReport = selectReportFromSeq(Seq(AttachmentOutput(FakeFile, FakeFileName))))

    val expectedResult = FakeUploadReportTask.copy(out = expectedOutput)

    val _ = result shouldBe expectedResult
  }

}

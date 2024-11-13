package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages

import javax.inject.Inject

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.UploadReportException
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages.DownloadReportStage.selectReportFromSeq
import com.neurowyzr.nw.dragon.service.biz.models.{AttachmentOutput, UploadReportTask}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.CreateTestSessionArgs
import com.neurowyzr.nw.dragon.service.clients.{AwsS3Client, CustomerHttpClient}
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

private[impl] class DownloadReportStage @Inject() (awsS3Client: AwsS3Client) extends FStage[UploadReportTask] {

  override def execute(task: UploadReportTask): Future[UploadReportTask] = {
    val futures = task.in.s3Urls.map { url =>
      val name             = awsS3Client.getFileName(url)
      val futureBytestream = awsS3Client.fetchFileFromS3(url)
      for {
        byteStream <- futureBytestream
      } yield AttachmentOutput(byteStream, name)
    }

    Future.collect(futures).map { (files: Seq[AttachmentOutput]) =>
      val output = task.out.copy(maybeReport = selectReportFromSeq(files))
      task.copy(out = output)
    }

  }

}

object DownloadReportStage {

  // todo: implement function to select file from sequence
  def selectReportFromSeq(files: Seq[AttachmentOutput]): Option[AttachmentOutput] = {
    files.headOption
  }

}

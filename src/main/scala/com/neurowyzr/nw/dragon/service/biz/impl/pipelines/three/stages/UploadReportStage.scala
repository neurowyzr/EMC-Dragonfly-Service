package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages

import javax.inject.Inject

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.UploadReportException
import com.neurowyzr.nw.dragon.service.biz.models.{NotifyClientTask, UploadReportTask}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.CreateTestSessionArgs
import com.neurowyzr.nw.dragon.service.clients.CustomerHttpClient
import com.neurowyzr.nw.dragon.service.data.{EpisodeRepository, UserBatchLookupRepository}
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import io.scalaland.chimney.dsl.TransformationOps

private[impl] class UploadReportStage @Inject() (customerHttpClient: CustomerHttpClient)
    extends FStage[UploadReportTask] {

  override def execute(task: UploadReportTask): Future[UploadReportTask] = {
    val maybeReport = task.out.maybeReport
    val maybeArgs: Option[CreateTestSessionArgs] =
      for {
        requestId  <- task.out.maybeRequestId
        episodeRef <- task.out.maybeEpisodeRef
        patientRef <- task.out.maybePatientRef
        locationId <- task.out.maybeLocationId
      } yield CreateTestSessionArgs(requestId, patientRef, episodeRef, locationId)

    maybeArgs match {
      case Some(args) =>
        maybeReport match {
          case Some(report) => customerHttpClient.uploadReport(args, report.bytestream).map(_ => task)
          case None =>
            val reason = "Unexpected error: report is empty"
            abort(UploadReportException(reason))
        }
      case None =>
        val reason = "Unexpected error: requestId, episodeRef, patientRef or locationId is empty"
        abort(UploadReportException(reason))
    }
  }

}

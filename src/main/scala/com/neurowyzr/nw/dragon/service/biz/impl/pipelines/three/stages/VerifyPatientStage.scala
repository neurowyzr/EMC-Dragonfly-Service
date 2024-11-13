package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages

import javax.inject.Inject

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.UploadReportException
import com.neurowyzr.nw.dragon.service.biz.models.UploadReportTask
import com.neurowyzr.nw.dragon.service.data.{EpisodeRepository, UserRepository}
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.softwaremill.quicklens.ModifyPimp

private[impl] class VerifyPatientStage @Inject() (userRepo: UserRepository) extends FStage[UploadReportTask] {

  override def execute(task: UploadReportTask): Future[UploadReportTask] = {

    task.out.maybeUserId match {
      case Some(userId) =>
        userRepo.getUserById(userId).flatMap {
          case Some(user) =>
            user.maybeExternalPatientRef match {
              case Some(externalPatientRef) =>
                Future.value(task.modify(_.out.maybePatientRef).setTo(Some(externalPatientRef)))
              case None =>
                val reason = s"Patient ref does not exist for user id: ${userId.toString}"
                abort(UploadReportException(reason))
            }
          case None =>
            val reason = s"User does not exist for user id: ${userId.toString}"
            abort(UploadReportException(reason))
        }
      case None =>
        val reason = "Unexpected error: empty user id"
        abort(UploadReportException(reason))
    }
  }

}

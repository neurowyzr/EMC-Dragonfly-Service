package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages

import javax.inject.Inject

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.UploadReportException
import com.neurowyzr.nw.dragon.service.biz.models.UploadReportTask
import com.neurowyzr.nw.dragon.service.data.UserBatchLookupRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.softwaremill.quicklens.ModifyPimp

private[impl] class VerifyUserBatchStage @Inject() (userBatchLookupRepo: UserBatchLookupRepository)
    extends FStage[UploadReportTask] {

  override def execute(task: UploadReportTask): Future[UploadReportTask] = {
    val userBatchCode = task.in.userBatchCode

    userBatchLookupRepo.getUserBatchLookupByCode(userBatchCode).flatMap {
      case Some(userBatchLookup) => Future.value(task.modify(_.out.maybeLocationId).setTo(Some(userBatchLookup.key)))
      case None =>
        val reason = s"Location id does not exist for user batch code: $userBatchCode"
        abort(UploadReportException(reason))
    }
  }

}

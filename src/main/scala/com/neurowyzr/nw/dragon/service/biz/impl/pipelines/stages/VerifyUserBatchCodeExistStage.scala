package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.ErrorOutcomeException
import com.neurowyzr.nw.dragon.service.biz.models.CreateMagicLinkTask
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.UserBatchCodeMissing
import com.neurowyzr.nw.dragon.service.data.UserBatchRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.softwaremill.quicklens.*

private[impl] class VerifyUserBatchCodeExistStage(userBatchRepo: UserBatchRepository)
    extends FStage[CreateMagicLinkTask] {

  override def execute(task: CreateMagicLinkTask): Future[CreateMagicLinkTask] = {
    userBatchRepo.getUserBatchByCode(task.in.userBatchCode).flatMap {
      case Some(userBatch) => Future.value(task.modify(_.out.userBatchId).setTo(Some(userBatch.id)))
      case None =>
        val reason = s"User batch code: ${task.in.userBatchCode} not found."
        abort(ErrorOutcomeException(UserBatchCodeMissing, reason))
    }
  }

}

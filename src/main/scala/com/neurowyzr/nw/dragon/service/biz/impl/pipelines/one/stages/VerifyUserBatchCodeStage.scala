package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import javax.inject.Inject

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.CreateTestSessionException
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus.UserBatchNotFound
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionTask
import com.neurowyzr.nw.dragon.service.data.UserBatchRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.softwaremill.quicklens.ModifyPimp

private[impl] class VerifyUserBatchCodeStage @Inject() (userBatchRepo: UserBatchRepository)
    extends FStage[CreateTestSessionTask] {

  override def execute(task: CreateTestSessionTask): Future[CreateTestSessionTask] = {
    val requestId = task.in.requestId
    task.out.maybeUserBatchCode match {
      case Some(userBatchCode) =>
        userBatchRepo.getUserBatchByCode(userBatchCode).flatMap {
          case Some(userBatch) => Future.value(task.modify(_.out.maybeUserBatchId).setTo(Some(userBatch.id)))
          case None =>
            val reason =
              s"User batch not found for user batch code: ${task.out.maybeUserBatchCode.get} and request id: $requestId."
            abort(CreateTestSessionException(UserBatchNotFound, reason))
        }
      case None =>
        val reason = s"User batch code is empty for request id $requestId."
        abort(CreateTestSessionException(UserBatchNotFound, reason))
    }
  }

}

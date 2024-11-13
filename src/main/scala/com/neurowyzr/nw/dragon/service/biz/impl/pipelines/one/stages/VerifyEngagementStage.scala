package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import javax.inject.Inject

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.CreateTestSessionException
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus.EngagementNotFound
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionTask
import com.neurowyzr.nw.dragon.service.data.EngagementRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.softwaremill.quicklens.ModifyPimp

private[impl] class VerifyEngagementStage @Inject() (engagementRepo: EngagementRepository)
    extends FStage[CreateTestSessionTask] {

  override def execute(task: CreateTestSessionTask): Future[CreateTestSessionTask] = {
    val requestId     = task.in.requestId
    val userBatchCode = task.out.maybeUserBatchCode.get

    engagementRepo.getEngagementByUserBatchCode(userBatchCode).flatMap {
      case Some(engagement) => Future.value(task.modify(_.out.maybeEngagementId).setTo(Some(engagement.id)))
      case None =>
        val reason = s"Engagement not found for user batch code: $userBatchCode and request id: $requestId."
        abort(CreateTestSessionException(EngagementNotFound, reason))
    }
  }

}

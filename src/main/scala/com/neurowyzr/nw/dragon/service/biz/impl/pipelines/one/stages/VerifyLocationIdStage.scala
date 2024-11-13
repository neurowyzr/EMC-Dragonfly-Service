package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import javax.inject.Inject

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.CreateTestSessionException
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus.InvalidLocationId
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionTask
import com.neurowyzr.nw.dragon.service.data.UserBatchLookupRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.softwaremill.quicklens.ModifyPimp

private[impl] class VerifyLocationIdStage @Inject() (userBatchLookupRepo: UserBatchLookupRepository)
    extends FStage[CreateTestSessionTask] {

  override def execute(task: CreateTestSessionTask): Future[CreateTestSessionTask] = {
    val locationId = task.in.locationId
    val requestId  = task.in.requestId
    userBatchLookupRepo
      .getUserBatchLookupByKey(locationId)
      .flatMap(maybeUserBatchLookup =>
        maybeUserBatchLookup match {
          case Some(userBatchLookup) =>
            Future.value(task.modify(_.out.maybeUserBatchCode).setTo(Some(userBatchLookup.value)))
          case None =>
            val reason = s"User batch code not found for location id: $locationId and request id: $requestId."
            abort(CreateTestSessionException(InvalidLocationId, reason))
        }
      )
  }

}

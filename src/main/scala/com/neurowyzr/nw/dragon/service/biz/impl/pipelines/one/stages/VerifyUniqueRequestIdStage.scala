package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.CreateTestSessionException
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus.DuplicateRequestId
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionTask
import com.neurowyzr.nw.dragon.service.data.EpisodeRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

private[impl] class VerifyUniqueRequestIdStage(episodeRepo: EpisodeRepository) extends FStage[CreateTestSessionTask] {

  override def execute(task: CreateTestSessionTask): Future[CreateTestSessionTask] = {
    val messageId = task.in.requestId
    val source    = task.in.source

    episodeRepo.getEpisodeByMessageIdAndSource(messageId, source).flatMap {
      case Some(_) =>
        val reason = s"Request id: $messageId and source: $source already exists"
        abort(CreateTestSessionException(DuplicateRequestId, reason))
      case _ => Future.value(task)
    }
  }

}

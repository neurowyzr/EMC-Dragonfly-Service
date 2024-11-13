package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import javax.inject.Inject

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.CreateTestSessionException
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus.DuplicateEpisodeId
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionTask
import com.neurowyzr.nw.dragon.service.data.EpisodeRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

private[impl] class VerifyUniqueEpisodeIdStage @Inject() (episodeRepo: EpisodeRepository)
    extends FStage[CreateTestSessionTask] {

  override def execute(task: CreateTestSessionTask): Future[CreateTestSessionTask] = {
    val episodeRef = task.in.episodeRef
    val source     = task.in.source
    val requestId  = task.in.requestId

    episodeRepo.getEpisodeByEpisodeRefAndSource(episodeRef, source).flatMap {
      case Some(_) =>
        val reason =
          s"Episode ref: $episodeRef already exists, returning previously generated magic link with request id: $requestId."
        abort(CreateTestSessionException(DuplicateEpisodeId, reason))
      case _ => Future.value(task)
    }
  }

}

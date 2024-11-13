package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException
import com.neurowyzr.nw.dragon.service.biz.models.CreateMagicLinkTask
import com.neurowyzr.nw.dragon.service.data.EpisodeRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

private[impl] class CheckDuplicateMessageIdStage(episodeRepo: EpisodeRepository) extends FStage[CreateMagicLinkTask] {

  override def execute(task: CreateMagicLinkTask): Future[CreateMagicLinkTask] = {
    episodeRepo.getEpisodeByMessageId(task.ctx.messageId).flatMap {
      case Some(_) => abort(BizException(s"Message id: ${task.ctx.messageId} exists. This is a duplicate message."))
      case None    => skip(task)
    }
  }

}

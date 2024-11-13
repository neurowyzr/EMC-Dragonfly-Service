package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.ErrorOutcomeException
import com.neurowyzr.nw.dragon.service.biz.models.*
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.EngagementNotFound
import com.neurowyzr.nw.dragon.service.data.{EngagementRepository, EpisodeRepository}
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.softwaremill.quicklens.ModifyPimp

private[impl] class CreateMagicLinkStage(episodeRepo: EpisodeRepository, engagementRepo: EngagementRepository)
    extends FStage[CreateMagicLinkTask] {

  override def execute(task: CreateMagicLinkTask): Future[CreateMagicLinkTask] = {
    task.out match {
      case CreateMagicLinkTaskOutput(Some(userId), Some(userBatchId), _) =>
        engagementRepo.getEngagementByUserBatchCode(task.in.userBatchCode).flatMap {
          case Some(engagement) =>
            val newTestSession = TestSession(userId, userBatchId, engagement.id)
            val newEpisode = Episode(
              userId = userId,
              episodeRef = task.in.testId,
              messageId = task.ctx.messageId,
              source = task.in.source,
              utcStartAt = task.in.startDate,
              utcExpiryAt = task.in.expiryDate
            )
            episodeRepo
              .insertEpisodeAndTestSession(newEpisode, newTestSession)
              .map(_ => task.modify(_.out.maybeOutcome).setTo(Some(Outcomes.Success)))
          case _ =>
            val reason = s"Engagement doesn't exist for this user batch code: ${task.in.userBatchCode}"
            abort(ErrorOutcomeException(EngagementNotFound, reason))
        }
    }

  }

}

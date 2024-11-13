package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.{Outcomes, UpdateMagicLinkTask}
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.EpisodeNotFound
import com.neurowyzr.nw.dragon.service.data.EpisodeRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.softwaremill.quicklens.ModifyPimp

private[impl] class UpdateMagicLinkStage(episodeRepository: EpisodeRepository) extends FStage[UpdateMagicLinkTask] {

  override def execute(task: UpdateMagicLinkTask): Future[UpdateMagicLinkTask] = {
    val expiryDate   = task.in.expiryDate
    val maybeEpisode = episodeRepository.getEpisodeByTestId(task.in.testId)
    maybeEpisode.flatMap {
      case Some(episode) =>
        episodeRepository
          .updateEpisodeExpiryDate(episode.id, expiryDate)
          .map(_ => task.modify(_.out.maybeOutcome).setTo(Some(Outcomes.Success)))
      case None =>
        error(s"Unable to find episode for test id: '${task.in.testId}'.")
        Future.value(task.modify(_.out.maybeOutcome).setTo(Some(EpisodeNotFound)))
    }
  }

}

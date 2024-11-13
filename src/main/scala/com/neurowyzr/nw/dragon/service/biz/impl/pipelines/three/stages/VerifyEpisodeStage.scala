package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.stages

import javax.inject.Inject

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.UploadReportException
import com.neurowyzr.nw.dragon.service.biz.models.{NotifyClientTask, UploadReportTask}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.CreateTestSessionArgs
import com.neurowyzr.nw.dragon.service.clients.CustomerHttpClient
import com.neurowyzr.nw.dragon.service.data.{EpisodeRepository, UserBatchLookupRepository}
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.softwaremill.quicklens.ModifyPimp
import io.scalaland.chimney.dsl.TransformationOps

private[impl] class VerifyEpisodeStage @Inject() (episodeRepo: EpisodeRepository) extends FStage[UploadReportTask] {

  override def execute(task: UploadReportTask): Future[UploadReportTask] = {
    val episodeId = task.in.episodeId

    episodeRepo.getEpisodeById(episodeId).flatMap {
      case Some(episode) =>
        episode.maybeMessageId match {
          case Some(messageId) =>
            Future.value(
              task
                .modify(_.out.maybeRequestId)
                .setTo(Some(messageId))
                .modify(_.out.maybeUserId)
                .setTo(Some(episode.userId))
                .modify(_.out.maybeEpisodeRef)
                .setTo(Some(episode.episodeRef))
            )
          case None =>
            val reason = s"Message id does not exist for episode with episode id: ${episodeId.toString}"
            abort(UploadReportException(reason))
        }
      case None =>
        val reason = s"Episode does not exist for episode id: ${episodeId.toString}"
        abort(UploadReportException(reason))
    }
  }

}

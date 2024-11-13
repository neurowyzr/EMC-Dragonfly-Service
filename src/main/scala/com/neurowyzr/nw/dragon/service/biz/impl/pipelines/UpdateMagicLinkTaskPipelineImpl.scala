package com.neurowyzr.nw.dragon.service.biz.impl.pipelines

import javax.inject.{Inject, Singleton}

import com.neurowyzr.nw.dragon.service.biz.UpdateMagicLinkTaskPipeline
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.{
  DuplicateEventFilterStage, NotifyCygnusStage, UpdateMagicLinkStage
}
import com.neurowyzr.nw.dragon.service.biz.models.UpdateMagicLinkTask
import com.neurowyzr.nw.dragon.service.data.{CygnusEventRepository, EpisodeRepository}
import com.neurowyzr.nw.dragon.service.mq.impl.CygnusPublisherImpl
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

@Singleton
class UpdateMagicLinkTaskPipelineImpl @Inject() (episodeRepository: EpisodeRepository,
                                                 cygnusRepo: CygnusEventRepository,
                                                 producer: CygnusPublisherImpl
                                                )
    extends UpdateMagicLinkTaskPipeline {

  override protected val allStages: List[FStage[UpdateMagicLinkTask]] = List(
    new DuplicateEventFilterStage(cygnusRepo),
    new UpdateMagicLinkStage(episodeRepository),
    new NotifyCygnusStage[UpdateMagicLinkTask](producer)
  )

}

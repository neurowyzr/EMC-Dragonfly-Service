package com.neurowyzr.nw.dragon.service.biz.impl.pipelines

import javax.inject.{Inject, Singleton}

import com.neurowyzr.nw.dragon.service.biz.InvalidateMagicLinkTaskPipeline
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.{
  DuplicateEventFilterStage, InvalidateMagicLinkStage, NotifyCygnusStage
}
import com.neurowyzr.nw.dragon.service.biz.models.InvalidateMagicLinkTask
import com.neurowyzr.nw.dragon.service.data.{CygnusEventRepository, EpisodeRepository}
import com.neurowyzr.nw.dragon.service.mq.impl.CygnusPublisherImpl
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

@Singleton
class InvalidateMagicLinkTaskPipelineImpl @Inject() (episodeRepository: EpisodeRepository,
                                                     cygnusRepo: CygnusEventRepository,
                                                     producer: CygnusPublisherImpl
                                                    )
    extends InvalidateMagicLinkTaskPipeline {

  override protected val allStages: List[FStage[InvalidateMagicLinkTask]] = List(
    new DuplicateEventFilterStage(cygnusRepo),
    new InvalidateMagicLinkStage(episodeRepository),
    new NotifyCygnusStage[InvalidateMagicLinkTask](producer)
  )

}

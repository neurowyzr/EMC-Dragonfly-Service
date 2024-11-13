package com.neurowyzr.nw.dragon.service.biz.impl.pipelines

import javax.inject.{Inject, Singleton}

import com.neurowyzr.nw.dragon.service.biz.CreateMagicLinkTaskPipeline
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.*
import com.neurowyzr.nw.dragon.service.biz.models.CreateMagicLinkTask
import com.neurowyzr.nw.dragon.service.data.*
import com.neurowyzr.nw.dragon.service.mq.impl.CygnusPublisherImpl
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

@Singleton
class CreateMagicLinkTaskPipelineImpl @Inject() (episodeRepo: EpisodeRepository,
                                                 userRepo: UserRepository,
                                                 userBatchRepo: UserBatchRepository,
                                                 engagementRepo: EngagementRepository,
                                                 userAccountRepo: UserAccountRepository,
                                                 cygnusRepo: CygnusEventRepository,
                                                 producer: CygnusPublisherImpl
                                                )
    extends CreateMagicLinkTaskPipeline {

  override protected val allStages: List[FStage[CreateMagicLinkTask]] = List(
    new DuplicateEventFilterStage(cygnusRepo),
    new VerifyUserExistStage(userRepo),
    new VerifyUserBatchCodeExistStage(userBatchRepo),
    new CreateUserAccountStage(userAccountRepo),
    new CreateMagicLinkStage(episodeRepo, engagementRepo),
    new NotifyCygnusStage[CreateMagicLinkTask](producer)
  )

}

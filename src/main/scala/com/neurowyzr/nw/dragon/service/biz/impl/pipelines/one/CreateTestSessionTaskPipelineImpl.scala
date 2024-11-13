package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one

import javax.inject.{Inject, Singleton}

import com.neurowyzr.nw.dragon.service.biz.CreateTestSessionTaskPipeline
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages.*
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionTask
import com.neurowyzr.nw.dragon.service.cfg.Models.{CustomerConfig, DbfsConfig}
import com.neurowyzr.nw.dragon.service.clients.CoreHttpClient
import com.neurowyzr.nw.dragon.service.data.{
  EngagementRepository, EpisodeRepository, RevInfoRepository, UserAccountAudRepository, UserAccountRepository,
  UserBatchLookupRepository, UserBatchRepository, UserRepository, UserRoleRepository
}
import com.neurowyzr.nw.dragon.service.mq.SelfPublisher
import com.neurowyzr.nw.finatra.lib.clients.AlerterHttpClient
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

@Singleton
class CreateTestSessionTaskPipelineImpl @Inject() (episodeRepo: EpisodeRepository,
                                                   userBatchLookupRepo: UserBatchLookupRepository,
                                                   userBatchRepo: UserBatchRepository,
                                                   engagementRepo: EngagementRepository,
                                                   userRepo: UserRepository,
                                                   userAccountRepo: UserAccountRepository,
                                                   userAccountAudRepo: UserAccountAudRepository,
                                                   userRoleRepo: UserRoleRepository,
                                                   revInfoRepo: RevInfoRepository,
                                                   dbfsConfig: DbfsConfig,
                                                   customerConfig: CustomerConfig,
                                                   coreHttpClient: CoreHttpClient,
                                                   alerterHttpClient: AlerterHttpClient,
                                                   selfPublisher: SelfPublisher
                                                  )
    extends CreateTestSessionTaskPipeline {

  override protected val allStages: List[FStage[CreateTestSessionTask]] = List(
    new VerifyUniqueRequestIdStage(episodeRepo),
    new VerifyUniqueEpisodeIdStage(episodeRepo),
    new VerifyLocationIdStage(userBatchLookupRepo),
    new VerifyUserBatchCodeStage(userBatchRepo),
    new VerifyEngagementStage(engagementRepo),
    new CreateUserStage(userRepo, dbfsConfig, customerConfig),
    new CreateTestSessionStage(userRoleRepo,
                               userAccountRepo,
                               revInfoRepo,
                               userAccountAudRepo,
                               episodeRepo,
                               dbfsConfig,
                               customerConfig,
                               coreHttpClient
                              )
  )

}

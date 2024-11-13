package com.neurowyzr.nw.dragon.service.biz.impl.pipelines

import javax.inject.{Inject, Singleton}

import com.neurowyzr.nw.dragon.service.biz.CreateUserTaskPipeline
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.{
  CreateUserStage, DuplicateEventFilterStage, NotifyCygnusStage, VerifyPatientRefExistStage
}
import com.neurowyzr.nw.dragon.service.biz.models.CreateUserTask
import com.neurowyzr.nw.dragon.service.cfg.Models.DbfsConfig
import com.neurowyzr.nw.dragon.service.data.{CygnusEventRepository, UserRepository}
import com.neurowyzr.nw.dragon.service.mq.impl.CygnusPublisherImpl
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

@Singleton
class CreateUserTaskPipelineImpl @Inject() (userRepository: UserRepository,
                                            cygnusRepo: CygnusEventRepository,
                                            dbfsConfig: DbfsConfig,
                                            producer: CygnusPublisherImpl
                                           )
    extends CreateUserTaskPipeline {

  override protected val allStages: List[FStage[CreateUserTask]] = List(
    new DuplicateEventFilterStage(cygnusRepo),
    new VerifyPatientRefExistStage(userRepository),
    new CreateUserStage(userRepository, dbfsConfig),
    new NotifyCygnusStage[CreateUserTask](producer)
  )

}

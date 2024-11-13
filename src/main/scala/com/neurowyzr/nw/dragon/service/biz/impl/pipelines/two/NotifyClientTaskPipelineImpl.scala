package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.two

import javax.inject.Inject

import com.neurowyzr.nw.dragon.service.biz.NotifyClientTaskPipeline
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.two.stages.NotifyClientStage
import com.neurowyzr.nw.dragon.service.biz.models.NotifyClientTask
import com.neurowyzr.nw.dragon.service.clients.CustomerHttpClient
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

class NotifyClientTaskPipelineImpl @Inject() (customerHttpClient: CustomerHttpClient) extends NotifyClientTaskPipeline {

  override protected val allStages: List[FStage[NotifyClientTask]] = List(
    new NotifyClientStage(customerHttpClient)
  )

}

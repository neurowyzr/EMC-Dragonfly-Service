package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException
import com.neurowyzr.nw.dragon.service.biz.models.Task
import com.neurowyzr.nw.dragon.service.mq.impl.CygnusPublisherImpl
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

private[impl] class NotifyCygnusStage[T <: Task](producer: CygnusPublisherImpl) extends FStage[T] {

  override def execute(task: T): Future[T] = {

    task.out.maybeOutcome match {
      case Some(outcome) => producer.publishOutcome(task.ctx, outcome).map(_ => task)
      case _             => abort(BizException(s"There is no outcome for task ${task.ctx.messageId}"))
    }
  }

}

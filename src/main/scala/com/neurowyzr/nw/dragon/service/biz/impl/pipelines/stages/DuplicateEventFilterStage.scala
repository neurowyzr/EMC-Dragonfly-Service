package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException
import com.neurowyzr.nw.dragon.service.biz.models.{CygnusEvent, Task}
import com.neurowyzr.nw.dragon.service.data.CygnusEventRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

private[impl] class DuplicateEventFilterStage[T <: Task](cygnusRepo: CygnusEventRepository) extends FStage[T] {

  override def execute(task: T): Future[T] = {

    (task.ctx.maybeMessageId, task.ctx.maybeType) match {
      case (Some(messageId), Some(messageType)) =>
        cygnusRepo.getCygnusEventByMessageTypeAndMessageId(messageType, messageId).flatMap {
          case Some(_) => abort(BizException(s"Duplicate event: '$messageId'"))
          case None    => cygnusRepo.createCygnusEvent(CygnusEvent(messageType, messageId)).flatMap(_ => next(task))
        }
      case _ => abort(BizException(s"Missing messageId and/or messageType in task: '${task.toString}'"))
    }
  }

}

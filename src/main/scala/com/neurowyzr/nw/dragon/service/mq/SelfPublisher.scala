package com.neurowyzr.nw.dragon.service.mq

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.{Outcome, TaskContext}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.EnqueueTestSessionParams
import com.neurowyzr.nw.dragon.service.mq.impl.SelfPublisherImpl.PublishParams

trait SelfPublisher {
  def publishTestSessionMessage(params: EnqueueTestSessionParams): Future[String]
  def publishSuccess(task: TaskContext, magicLinkUrl: String, params: PublishParams): Future[Unit]
  def publishFailure(task: TaskContext, params: PublishParams): Future[Unit]

}

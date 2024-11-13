package com.neurowyzr.nw.dragon.service.mq

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.{Outcome, TaskContext}

trait CygnusPublisher {
  def publishOutcome(context: TaskContext, outcome: Outcome): Future[Unit]

}

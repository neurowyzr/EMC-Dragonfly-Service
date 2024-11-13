package com.neurowyzr.nw.dragon.service.clients

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.{CreateTestSessionArgs, EnqueueTestSessionParams}

trait CustomerHttpClient {
  def notifyCreateSucceeded(args: CreateTestSessionArgs, magicLink: String): Future[Unit]
  def notifyCreateFailed(args: CreateTestSessionArgs, statusCode: Int): Future[Unit]
  def uploadReport(args: CreateTestSessionArgs, report: Array[Byte]): Future[Unit]
}

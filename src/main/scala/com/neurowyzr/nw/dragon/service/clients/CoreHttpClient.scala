package com.neurowyzr.nw.dragon.service.clients

import com.twitter.finagle.http.Response
import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.MagicLinkTestSessionDetail
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.{CurrentTestSession, SendUserReport}
import com.neurowyzr.nw.dragon.service.biz.models.UserModels.LatestReportParams

trait CoreHttpClient {
  def sendUserReport(params: SendUserReport): Future[Response]
  def getCurrentTestSession(params: CurrentTestSession): Future[MagicLinkTestSessionDetail]
  def sendLatestReport(params: LatestReportParams): Future[Response]
}

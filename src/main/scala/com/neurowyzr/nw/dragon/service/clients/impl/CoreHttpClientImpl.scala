package com.neurowyzr.nw.dragon.service.clients.impl

import javax.inject.Inject

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finagle.http.{Response, Status}
import com.twitter.finatra.http.request.RequestBuilder
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException
import com.neurowyzr.nw.dragon.service.biz.models.{MagicLinkTestSessionDetail, UserModels}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.{CurrentTestSession, SendUserReport}
import com.neurowyzr.nw.dragon.service.clients.CoreHttpClient

import com.google.inject.Singleton

@Singleton
class CoreHttpClientImpl @Inject() (client: HttpClient) extends CoreHttpClient {

  override def sendUserReport(params: SendUserReport): Future[Response] = {
    val basePath = s"/api/v1/user-batch-codes/${params.userBatchCode}/sessions/${params.sessionId}/result"

    val request = RequestBuilder.post(basePath)

    client.execute(request)
  }

  override def getCurrentTestSession(params: CurrentTestSession): Future[MagicLinkTestSessionDetail] = {

    val mapper = (new ScalaObjectMapperModule).camelCaseObjectMapper

    val eId  = params.engagementId.toString
    val ubId = params.userBatchId.toString
    val uId  = params.userId.toString

    val basePath = s"/api/v1/magic-links/current-test-session/engagementId/$eId/userBatchId/$ubId/userId/$uId"

    val request = RequestBuilder.get(basePath)

    val response: Response = Await.result(client.execute(request), 5.second)

    if (response.status == Status.Ok) {
      try {
        val detail = mapper.parse[MagicLinkTestSessionDetail](response.contentString)
        Future.value(detail)
      } catch {
        case e: Exception => Future.exception(BizException("Error parsing test session detail: " + e.getMessage))
      }
    } else {
      Future.exception(
        BizException("Error retrieving test session detail from core. Status: " + response.status.toString)
      )
    }
  }

  override def sendLatestReport(params: UserModels.LatestReportParams): Future[Response] = {

    val userBatchCode = params.userBatchCode
    val sessionId     = params.sessionId

    val basePath = s"/api/v1/magic-links/user-batch-codes/$userBatchCode/sessions/$sessionId/result"

    val request            = RequestBuilder.post(basePath)
    val response: Response = Await.result(client.execute(request), 5.second)

    if (response.status == Status.Ok || response.status == Status.NotFound) {
      Future.value(response)
    } else {
      Future.exception(
        BizException("Error sending report to user. Response from core http client " + response.status.toString)
      )
    }
  }

}

package com.neurowyzr.nw.dragon.service.clients.impl

import java.time.OffsetDateTime
import java.util.Date

import com.twitter.finagle.http.{Response, Status}
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.httpclient.test.InMemoryHttpService
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.util.Await

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeSessionId, FakeUserBatchCode}
import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException
import com.neurowyzr.nw.dragon.service.biz.models.MagicLinkTestSessionDetail
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.{CurrentTestSession, SendUserReport}
import com.neurowyzr.nw.dragon.service.biz.models.UserModels.LatestReportParams
import com.neurowyzr.nw.dragon.service.clients.impl.CoreHttpClientImpl

import org.mockito.ArgumentMatchersSugar
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class CoreHttpClientImplTest extends AnyFunSuite with ArgumentMatchersSugar with Matchers with BeforeAndAfter {

  private val inMemoryHttpService = new InMemoryHttpService()
  private val testMapper          = (new ScalaObjectMapperModule).objectMapper
  private val testClient          = new HttpClient(httpService = inMemoryHttpService, mapper = testMapper)
  private val testInstance        = new CoreHttpClientImpl(testClient)

  after {
    inMemoryHttpService.clear()
  }

  test("POST /users/:userId/sessions with correct parameters") {
    val okResponse = Response(Status.Ok)
    val userId     = "fake-user-id"
    val testPath   = s"/api/v1/user-batch-codes/$FakeUserBatchCode/sessions/$FakeSessionId/result"
    inMemoryHttpService.mockPost(testPath, "", andReturn = okResponse)

    val params = SendUserReport(FakeSessionId, FakeUserBatchCode)

    Await.result(testInstance.sendUserReport(params)) should be(okResponse)
  }

  test("GET /magic-links/current-test-session with correct parameters returns valid session details") {
    val okResponse = Response(Status.Ok)
    okResponse.setContentString(
      "{ \"userId\": 665, \"userBatchId\": 213, \"frequency\": \"SINGLE\", \"testSessionOrder\": 1, \"testSessionStart\": \"2024-04-12T16:37:05.808+00:00\", \"testSessionEnd\": \"2025-04-12T16:00:00.000+00:00\", \"uacRevisionId\": null }"
    )

    val engagementId = 1L
    val userBatchId  = 2L
    val userId       = 3L
    val testPath =
      s"/api/v1/magic-links/current-test-session/engagementId/$engagementId/userBatchId/$userBatchId/userId/$userId"

    inMemoryHttpService.mockGet(testPath, andReturn = okResponse)

    val params = CurrentTestSession(engagementId, userBatchId, userId)

    val result: MagicLinkTestSessionDetail = Await.result(testInstance.getCurrentTestSession(params))

    result.userId shouldEqual 665
    result.userBatchId shouldEqual 213
    result.frequency shouldEqual "SINGLE"
    result.testSessionOrder shouldEqual 1
    result.testSessionStart shouldEqual Date.from(OffsetDateTime.parse("2024-04-12T16:37:05.808+00:00").toInstant)
    result.testSessionEnd shouldEqual Date.from(OffsetDateTime.parse("2025-04-12T16:00:00.000+00:00").toInstant)
  }

  test("GET /magic-links/current-test-session with incompatible parameters returns error") {
    val okResponse = Response(Status.Ok)
    okResponse.setContentString(
      "{ \"userId\": 665, \"userBatchId\": 213, \"frequency\": \"SINGLE\", \"testSessionOrder\": 1, \"testSessionStart\": \"2024-04-12T16:37:05.808+00:00\", \"WRONG_FIELD\": \"2025-04-12T16:00:00.000+00:00\", \"uacRevisionId\": null }"
    )

    val engagementId = 1L
    val userBatchId  = 2L
    val userId       = 3L
    val testPath =
      s"/api/v1/magic-links/current-test-session/engagementId/$engagementId/userBatchId/$userBatchId/userId/$userId"

    inMemoryHttpService.mockGet(testPath, andReturn = okResponse)

    val params = CurrentTestSession(engagementId, userBatchId, userId)

    val thrown = intercept[BizException] {
      Await.result(testInstance.getCurrentTestSession(params))
    }

    thrown.getMessage should include("Error parsing test session detail: ")
  }

  test("GET /magic-links/current-test-session with non-OK status returns error") {
    val errorResponse = Response(Status.InternalServerError)
    val engagementId  = 1L
    val userBatchId   = 2L
    val userId        = 3L
    val testPath =
      s"/api/v1/magic-links/current-test-session/engagementId/$engagementId/userBatchId/$userBatchId/userId/$userId"

    inMemoryHttpService.mockGet(testPath, andReturn = errorResponse)

    val params = CurrentTestSession(engagementId, userBatchId, userId)

    val thrown = intercept[BizException] {
      Await.result(testInstance.getCurrentTestSession(params))
    }

    thrown.getMessage should include("Error retrieving test session detail from core")
  }

  test("POST latest report returns ok") {
    val okResponse = Response(Status.Ok)
    okResponse.setContentString("{ \"link\": \"link-content\" }")

    val testPath = s"/api/v1/magic-links/user-batch-codes/$FakeUserBatchCode/sessions/$FakeSessionId/result"

    inMemoryHttpService.mockPost(testPath, andReturn = okResponse)

    val params = LatestReportParams(FakeUserBatchCode, FakeSessionId)

    Await.result(testInstance.sendLatestReport(params)) should be(okResponse)
  }

  test("POST latest report returns not found") {
    val notFoundResponse = Response(Status.NotFound)

    val testPath = s"/api/v1/magic-links/user-batch-codes/$FakeUserBatchCode/sessions/$FakeSessionId/result"

    inMemoryHttpService.mockPost(testPath, andReturn = notFoundResponse)

    val params = LatestReportParams(FakeUserBatchCode, FakeSessionId)

    Await.result(testInstance.sendLatestReport(params)) should be(notFoundResponse)
  }

  test("POST latest report throw exception not found") {
    val unAuthResponse = Response(Status.Unauthorized)

    val testPath = s"/api/v1/magic-links/user-batch-codes/$FakeUserBatchCode/sessions/$FakeSessionId/result"

    inMemoryHttpService.mockPost(testPath, andReturn = unAuthResponse)

    val params = LatestReportParams(FakeUserBatchCode, FakeSessionId)

    val thrown = intercept[BizException] {
      Await.result(testInstance.sendLatestReport(params))
    }

    thrown.getMessage should include("Error sending report to user. Response from core http client ")
  }

}

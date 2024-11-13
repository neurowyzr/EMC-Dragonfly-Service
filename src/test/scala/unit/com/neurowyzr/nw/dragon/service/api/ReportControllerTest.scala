package com.neurowyzr.nw.dragon.service.api

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status.{Ok, Unauthorized}
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.finatra.http.filters.ExceptionMappingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeInvalidHeaders, FakeValidHeaders}
import com.neurowyzr.nw.dragon.service.api.ReportControllerTest.TestServer
import com.neurowyzr.nw.dragon.service.biz.{ReportService, SessionService}
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeSessionOtp
import com.neurowyzr.nw.finatra.lib.api.mappers.AuthenticationExceptionMapper

import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}

class ReportControllerTest
    extends FeatureTest with IdiomaticMockito with ArgumentMatchersSugar with ResetMocksAfterEachTest {

  private final val mockService        = mock[ReportService]
  private final val mockSessionService = mock[SessionService]

  private lazy val testServer = new EmbeddedHttpServer(twitterServer = new TestServer, disableTestLogging = true)
    .bind[ReportService]
    .toInstance(mockService)
    .bind[SessionService]
    .toInstance(mockSessionService)

  override protected def server: EmbeddedHttpServer = testServer

  test("GET /v1/report/:session_id exist") {
    val _   = mockService.getReportPath(*[String]) returns "fake-path"
    val _   = mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))
    val url = "/v1/report/session_id"

    val response = server.httpGet(path = url, headers = FakeValidHeaders, andExpect = Ok)
    response.contentString shouldBe "fake-path"

    val _ = mockService.getReportPath(*[String]) wasCalled once
    val _ = mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    val _ = mockService wasNever calledAgain
    val _ = mockSessionService wasNever calledAgain
  }

  test("GET /v1/report/:session_id fails with invalid token") {
    val url = "/v1/report/session_id"

    val _ = server.httpGet(path = url, headers = FakeInvalidHeaders, andExpect = Unauthorized)

    val _ = mockService wasNever called
  }

}

private object ReportControllerTest {

  private class TestServer extends HttpServer {

    override protected def configureHttp(router: HttpRouter): Unit = {
      router
        .filter[ExceptionMappingFilter[Request]]
        .exceptionMapper[AuthenticationExceptionMapper]
        .add[ReportController]: Unit
    }

  }

}

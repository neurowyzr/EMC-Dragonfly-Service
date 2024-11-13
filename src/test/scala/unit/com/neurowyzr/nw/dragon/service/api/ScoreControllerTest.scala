package com.neurowyzr.nw.dragon.service.api

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status.{NotFound, Ok}
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.finatra.http.filters.ExceptionMappingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.SharedFakes.{fakeScoresResponse, FakeValidHeaders}
import com.neurowyzr.nw.dragon.service.api.ScoreController.GetScoresResponse
import com.neurowyzr.nw.dragon.service.api.ScoreControllerTest.TestServer
import com.neurowyzr.nw.dragon.service.biz.{ScoreService, SessionService}
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeSessionOtp
import com.neurowyzr.nw.finatra.lib.api.mappers.AuthenticationExceptionMapper

import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}

class ScoreControllerTest
    extends FeatureTest with IdiomaticMockito with ArgumentMatchersSugar with ResetMocksAfterEachTest {

  private final val mockService        = mock[ScoreService]
  private final val mockSessionService = mock[SessionService]
  private final val testMapper         = (new ScalaObjectMapperModule).objectMapper

  private lazy val testServer = new EmbeddedHttpServer(twitterServer = new TestServer, disableTestLogging = true)
    .bind[ScoreService]
    .toInstance(mockService)
    .bind[SessionService]
    .toInstance(mockSessionService)

  override protected def server: EmbeddedHttpServer = testServer

  test("GET /v1/scores/:user_batch_code exist") {
    val _   = mockService.getScores(*[String], *[String]) returns Future.value(Some(fakeScoresResponse))
    val _   = mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))
    val url = "/v1/scores/fakeco"

    val response       = server.httpGet(path = url, headers = FakeValidHeaders, andExpect = Ok)
    val parsedResponse = testMapper.parse[GetScoresResponse](response.contentString)
    parsedResponse.overallLatestScore shouldBe fakeScoresResponse.overallLatestScore

    val _ = mockService.getScores(*[String], *[String]) wasCalled once
    val _ = mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    val _ = mockService wasNever calledAgain
    val _ = mockSessionService wasNever calledAgain
  }

  test("GET /v1/scores/:user_batch_code doesn't exist") {
    val _ = mockService.getScores(*[String], *[String]) returns Future.None
    val _ = mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val url = "/v1/scores/fakeco"

    val _ = server.httpGet(path = url, headers = FakeValidHeaders, andExpect = NotFound)

    val _ = mockService.getScores(*[String], *[String]) wasCalled once
    val _ = mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    val _ = mockService wasNever calledAgain
    val _ = mockSessionService wasNever calledAgain
  }

}

private object ScoreControllerTest {

  private class TestServer extends HttpServer {

    override protected def configureHttp(router: HttpRouter): Unit = {
      router
        .filter[ExceptionMappingFilter[Request]]
        .exceptionMapper[AuthenticationExceptionMapper]
        .add[ScoreController]: Unit
    }

  }

}

package com.neurowyzr.nw.dragon.service.api

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status.*
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.finatra.http.filters.ExceptionMappingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.api.FakeCustomerControllerTest.*
import com.neurowyzr.nw.finatra.lib.api.filters.impl.DefaultAuthApiKeyHandler
import com.neurowyzr.nw.finatra.lib.api.mappers.AuthenticationExceptionMapper
import com.neurowyzr.nw.finatra.lib.biz.ApiKeyBasedAuthService

import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}

class FakeCustomerControllerTest
    extends FeatureTest with IdiomaticMockito with ArgumentMatchersSugar with ResetMocksAfterEachTest {

  private final val mockAuthService = mock[ApiKeyBasedAuthService]

  private lazy val testServer = new EmbeddedHttpServer(twitterServer = new TestServer, disableTestLogging = true)
    .bind[ApiKeyBasedAuthService]
    .toInstance(mockAuthService)

  override protected def server: EmbeddedHttpServer = testServer

  test("POST /api/User/authenticate returns 200 for valid username and password") {
    mockAuthService.tokenHandler returns DefaultAuthApiKeyHandler()
    mockAuthService.authenticate(*) returns Future(Map.empty)
    val url = "/api/User/authenticate"
    server.httpPost(path = url, postBody = FakeValidUsernamePasswordPayload, andExpect = Ok)

  }

  test("POST /v1/patients/:patient_id/episodes/:episode_id/LocationId/:location_id/status returns 200") {
    mockAuthService.tokenHandler returns DefaultAuthApiKeyHandler()
    mockAuthService.authenticate(*) returns Future(Map.empty)
    val url = "/v1/patients/p12/episodes/e12/LocationId/l12/status"
    server.httpPost(path = url, postBody = FakeValidPayload, headers = FakeValidHeaders, andExpect = Ok)

  }

  test("POST /v1/patients/:patient_id/episodes/:episode_id/LocationId/:location_id/report returns 200") {
    mockAuthService.tokenHandler returns DefaultAuthApiKeyHandler()
    mockAuthService.authenticate(*) returns Future(Map.empty)
    val url = "/v1/patients/p12/episodes/e12/LocationId/l12/report"
    server.httpPost(path = url, postBody = FakePdfPayload, headers = FakeValidHeaders, andExpect = Ok)

  }

}

private object FakeCustomerControllerTest {

  final val FakeValidJwt: String = "fake-token"

  final val FakeValidBearerToken: String = "Bearer " + FakeValidJwt

  private final val FakeValidUsernamePasswordPayload =
    """
      |{
      |  "Username": "fake-username",
      |  "Password": "fake-password"
      |}
      |""".stripMargin

  private final val FakeValidHeaders: Map[String, String] = Map("Authorization" -> FakeValidBearerToken)

  private final val FakeValidPayload =
    """
      |{
      |  "status": 1,
      |  "link": "https://neurowyzr.com/sample-test-session/abc123"
      |}
      |""".stripMargin

  private final val FakePdfPayload =
    """
      |{
      |  "Base64Pdf": "SGVsbG8sIFdvcmxkIQ=="
      |}
      |""".stripMargin

  private class TestServer extends HttpServer {

    override protected def configureHttp(router: HttpRouter): Unit = {
      router
        .filter[ExceptionMappingFilter[Request]]
        .exceptionMapper[AuthenticationExceptionMapper]
        .exceptionMapper[BizExceptionMapper]
        .add[FakeCustomerController]: Unit
    }

  }

}

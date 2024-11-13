package com.neurowyzr.nw.dragon.service.api

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status.*
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.finatra.http.filters.ExceptionMappingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.SharedFakes.*
import com.neurowyzr.nw.dragon.service.api.UsersControllerTest.TestServer
import com.neurowyzr.nw.dragon.service.biz.{SessionService, UserService}
import com.neurowyzr.nw.dragon.service.biz.exceptions.{
  ReportNotFoundException, UserConsentNotFoundException, UserNotFoundException
}
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeSessionOtp
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.UpdateUserParams
import com.neurowyzr.nw.dragon.service.biz.models.UserConsentModels.CreateUserDataConsentParams
import com.neurowyzr.nw.finatra.lib.api.mappers.AuthenticationExceptionMapper

import org.mockito.ArgumentMatchersSugar
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}

class UsersControllerTest
    extends FeatureTest with IdiomaticMockito with ArgumentMatchersSugar with ResetMocksAfterEachTest {

  private final val mockService        = mock[UserService]
  private final val mockSessionService = mock[SessionService]

  private lazy val testServer = new EmbeddedHttpServer(twitterServer = new TestServer, disableTestLogging = true)
    .bind[UserService]
    .toInstance(mockService)
    .bind[SessionService]
    .toInstance(mockSessionService)

  override protected def server: EmbeddedHttpServer = testServer

  test("GET /v1/users succeeds") {
    val url = "/v1/users"
    mockService.getUserByUsername(*[String]) returns Future.value(FakeUserWithDataConsent)
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val _ = server.httpGet(path = url, headers = FakeValidHeaders, andExpect = Ok)

    mockService.getUserByUsername(*[String]) wasCalled once
    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
    mockSessionService wasNever calledAgain
  }

  test("GET /v1/users fails with invalid token") {
    val url = "/v1/users"

    val _ = server.httpGet(path = url, headers = FakeInvalidHeaders, andExpect = Unauthorized)
  }

  test("GET /v1/users returns not found when user does not exist") {
    val url = "/v1/users"
    mockService.getUserByUsername(*[String]) returns Future.exception(UserNotFoundException("User not found"))
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val msg = server.httpGet(path = url, headers = FakeValidHeaders, andExpect = NotFound)
    msg.contentString shouldBe "{\"errors\":[\"User not found\"]}"

    mockService.getUserByUsername(*[String]) wasCalled once
    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
  }

  test("GET /v1/users returns not found when user consent does not exist") {
    val url = "/v1/users"
    mockService.getUserByUsername(*[String]) returns Future.exception(
      UserConsentNotFoundException("User consent not found")
    )
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val msg = server.httpGet(path = url, headers = FakeValidHeaders, andExpect = NotFound)
    msg.contentString shouldBe "{\"errors\":[\"User consent not found\"]}"

    mockService.getUserByUsername(*[String]) wasCalled once
    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
    mockSessionService wasNever calledAgain

  }

  test("DELETE /v1/users succeeds") {
    val url = "/v1/users"
    mockService.deleteUserByUsername(*[String]) returns Future.Unit
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val _ = server.httpDelete(path = url, headers = FakeValidHeaders, andExpect = NoContent)
    mockService.deleteUserByUsername(*[String]) wasCalled once
    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
    mockSessionService wasNever calledAgain
  }

  test("DELETE /v1/users fails with invalid token") {
    val url = "/v1/users"

    val _ = server.httpDelete(path = url, headers = FakeInvalidHeaders, andExpect = Unauthorized)
  }

  test("DELETE /v1/users/consent succeeds") {
    val url = "/v1/users/consent"
    mockService.deleteUserConsentByUsername(*[String]) returns Future.Unit
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val _ = server.httpDelete(path = url, headers = FakeValidHeaders, andExpect = NoContent)

    mockService.deleteUserConsentByUsername(*[String]) wasCalled once
    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
    mockSessionService wasNever calledAgain
  }

  test("DELETE /v1/users/consent fails with invalid token") {
    val url = "/v1/users/consent"

    val _ = server.httpDelete(path = url, headers = FakeInvalidHeaders, andExpect = Unauthorized)
  }

  test("DELETE /v1/users/consent fails when user not found") {
    val url = "/v1/users/consent"

    mockService.deleteUserConsentByUsername(*) returns Future.exception(UserNotFoundException("User not found"))
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val msg = server.httpDelete(path = url, headers = FakeValidHeaders, andExpect = NotFound)
    msg.contentString shouldBe "{\"errors\":[\"User not found\"]}"

    mockService.deleteUserConsentByUsername(*[String]) wasCalled once
    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
    mockSessionService wasNever calledAgain
  }

  test("POST /v1/users/consent succeeds") {
    val url = "/v1/users/consent"

    val json =
      s"""
         |{
         |    "is_data_consent": true
         |}
         |""".stripMargin
    val captor = ArgCaptor[CreateUserDataConsentParams]
    mockService.createUserConsent(captor) returns Future.Unit
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val _ = server.httpPost(path = url, postBody = json, headers = FakeValidHeaders, andExpect = Created)

    mockService.createUserConsent(*[CreateUserDataConsentParams]) wasCalled once
    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
    mockSessionService wasNever calledAgain
  }

  test("POST /v1/users/consent fails when user not found") {
    val url = "/v1/users/consent"

    val json =
      s"""
         |{
         |    "is_data_consent": true
         |}
         |""".stripMargin
    val captor = ArgCaptor[CreateUserDataConsentParams]
    mockService.createUserConsent(captor) returns Future.exception(UserNotFoundException("User not found"))
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val msg = server.httpPost(path = url, postBody = json, headers = FakeValidHeaders, andExpect = NotFound)
    msg.contentString shouldBe "{\"errors\":[\"User not found\"]}"

    mockService.createUserConsent(*[CreateUserDataConsentParams]) wasCalled once
    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
    mockSessionService wasNever calledAgain
  }

  test("POST /v1/users/consent fails with invalid token") {
    val url = "/v1/users/consent"

    val json =
      s"""
         |{
         |    "is_data_consent": true
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, headers = FakeInvalidHeaders, andExpect = Unauthorized)
  }

  test("PUT /v1/users/:user_id succeeds") {
    val url = "/v1/users/consent"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "name": "$FakeName",
         |    "birth_year": "$FakeBirthYear",
         |    "gender": "$FakeGender"
         |}
         |""".stripMargin

    mockService.updateUser(*[UpdateUserParams]) returns Future.Unit
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val _ = server.httpPut(path = url, putBody = json, headers = FakeValidHeaders, andExpect = NoContent)

    mockService.updateUser(*[UpdateUserParams]) wasCalled once
    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
    mockSessionService wasNever calledAgain
  }

  test("PUT /v1/users/:user_id fails with invalid gender") {
    val url = "/v1/users/consent"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "name": "$FakeName",
         |    "birth_year": "$FakeBirthYear",
         |    "gender": "UNKNOWN"
         |}
         |""".stripMargin

    mockService.updateUser(*[UpdateUserParams]) returns Future.Unit
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val res = server.httpPut(path = url, putBody = json, headers = FakeValidHeaders, andExpect = BadRequest)
    res.contentString shouldBe "{\"errors\":[\"gender: Gender must be either 'MALE', 'FEMALE' or 'OTHERS'\"]}"

    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockSessionService wasNever calledAgain
  }

  test("PUT /v1/users/:user_id fails with invalid token") {
    val url = "/v1/users/userId"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "name": "$FakeName",
         |    "birth_year": "$FakeBirthYear",
         |    "gender": "$FakeGender"
         |}
         |""".stripMargin

    val _ = server.httpPut(path = url, putBody = json, headers = FakeInvalidHeaders, andExpect = Unauthorized)
  }

  test("PUT /v1/users/:user_id throw user not found exception") {
    val url = "/v1/users/userId"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "name": "$FakeName",
         |    "birth_year": "$FakeBirthYear",
         |    "gender": "$FakeGender"
         |}
         |""".stripMargin

    mockService.updateUser(*[UpdateUserParams]) returns Future.exception(UserNotFoundException("error-message"))
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val _ = server.httpPut(path = url, putBody = json, headers = FakeValidHeaders, andExpect = NotFound)

    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockSessionService wasNever calledAgain
  }

  test("POST /v1/users/reports/latest gives status 200") {
    val url = "/v1/users/reports/latest"

    val json =
      s"""
         |{
         |  "user_batch_code": "$FakeUserBatchCode"
         |}
         |""".stripMargin
    mockService.sendLatestReport(*[String], *[String]) returns Future.value("some-fake-string")
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val result = server.httpPost(
      path = url,
      postBody = json,
      headers = FakeValidHeaders,
      andExpect = Ok
    )

    result.contentString shouldBe "Report is send to username: test@example.com"
    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockSessionService wasNever calledAgain
  }

  test("POST /v1/users/reports/latest throw 404 for ReportNotFoundException") {
    val url = "/v1/users/reports/latest"

    val json =
      s"""
         |{
         |  "user_batch_code": "$FakeUserBatchCode"
         |}
         |""".stripMargin
    mockService.sendLatestReport(*[String], *[String]) returns Future.exception(
      ReportNotFoundException("error-message")
    )
    mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val result = server.httpPost(
      path = url,
      postBody = json,
      headers = FakeValidHeaders,
      andExpect = NotFound
    )

    result.contentString shouldBe "{\"errors\":[\"error-message\"]}"
    mockSessionService.getSessionOtp(*[String], *[String]) wasCalled once
    mockSessionService wasNever calledAgain
  }

}

private object UsersControllerTest {

  private class TestServer extends HttpServer {

    override protected def configureHttp(router: HttpRouter): Unit = {
      router
        .filter[ExceptionMappingFilter[Request]]
        .exceptionMapper[AuthenticationExceptionMapper]
        .add[UsersController]: Unit
    }

  }

}

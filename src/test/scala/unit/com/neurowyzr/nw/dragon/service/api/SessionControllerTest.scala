package com.neurowyzr.nw.dragon.service.api

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.http.Status.*
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.finatra.http.filters.ExceptionMappingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Future, Throw, Try}

import com.neurowyzr.nw.dragon.service.SharedFakes.*
import com.neurowyzr.nw.dragon.service.api.SessionControllerTest.{
  FakeInvalidApiKeyHeaders, FakeValidApiKeyHeaders, FakeValidPayload, TestServer
}
import com.neurowyzr.nw.dragon.service.biz.SessionService
import com.neurowyzr.nw.dragon.service.biz.exceptions.*
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeSessionOtp
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.*
import com.neurowyzr.nw.dragon.service.biz.models.TaskContext
import com.neurowyzr.nw.dragon.service.biz.models.UserSurveyModels.CreateUserSurveyParams
import com.neurowyzr.nw.dragon.service.mq.SelfPublisher
import com.neurowyzr.nw.finatra.lib.api.filters.ApiKeyBasedAuthFilter
import com.neurowyzr.nw.finatra.lib.api.filters.impl.DefaultAuthApiKeyHandler
import com.neurowyzr.nw.finatra.lib.api.mappers.AuthenticationExceptionMapper
import com.neurowyzr.nw.finatra.lib.biz.ApiKeyBasedAuthService

import org.mockito.ArgumentMatchersSugar
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}

class SessionControllerTest
    extends FeatureTest with IdiomaticMockito with ArgumentMatchersSugar with ResetMocksAfterEachTest {

  private final val mockService     = mock[SessionService]
  private final val mockAuthService = mock[ApiKeyBasedAuthService]

  private lazy val testServer = new EmbeddedHttpServer(twitterServer = new TestServer, disableTestLogging = true)
    .bind[SessionService]
    .toInstance(mockService)
    .bind[ApiKeyBasedAuthService]
    .toInstance(mockAuthService)

  override protected def server: EmbeddedHttpServer = testServer

  test("POST /v1/patients/:patient_id/episodes/:episode_id/test returns 401 for invalid api key") {
    mockAuthService.tokenHandler returns DefaultAuthApiKeyHandler()
    mockAuthService.authenticate(*) returns Future(Map.empty)
    mockService.enqueueTestSession(*[EnqueueTestSessionParams]) returns Future.value("")

    val url = "/v1/patients/P12345/episodes/E12345/test"

    val response = server.httpPost(path = url,
                                   postBody = FakeValidPayload,
                                   headers = FakeInvalidApiKeyHeaders,
                                   andExpect = Unauthorized
                                  )

  }

  test("POST /v1/patients/:patient_id/episodes/:episode_id/test returns 202") {
    mockAuthService.tokenHandler returns DefaultAuthApiKeyHandler()
    mockAuthService.authenticate(*) returns Future(Map.empty)
    mockService.enqueueTestSession(*[EnqueueTestSessionParams]) returns Future.value("")

    val url = "/v1/patients/P12345/episodes/E12345/test"

    val response = server.httpPost(path = url,
                                   postBody = FakeValidPayload,
                                   headers = FakeValidApiKeyHeaders,
                                   andExpect = Accepted
                                  )

  }

  test("POST /v1/patients/:patient_id/episodes/:episode_id/test with invalid patient_id and episode_id returns 400") {
    mockAuthService.tokenHandler returns DefaultAuthApiKeyHandler()
    mockAuthService.authenticate(*) returns Future(Map.empty)
    mockService.enqueueTestSession(*[EnqueueTestSessionParams]) returns Future.exception(
      BizException("Test exception")
    )

    val invalidPatientIdUrl = "/v1/patients/P1234/episodes/E12345/test"

    val responseInvalidUid = server.httpPost(path = invalidPatientIdUrl,
                                             postBody = FakeValidPayload,
                                             headers = FakeValidApiKeyHeaders,
                                             andExpect = BadRequest
                                            )

    val invalidEpisodeIdUrl = "/v1/patients/P12345/episodes/E1234/test"

    val responseInvalidAhc = server.httpPost(path = invalidEpisodeIdUrl,
                                             postBody = FakeValidPayload,
                                             headers = FakeValidApiKeyHeaders,
                                             andExpect = BadRequest
                                            )

  }

  test("POST /v1/patients/:patient_id/episodes/:episode_id/test with missing payload fields") {
    mockAuthService.tokenHandler returns DefaultAuthApiKeyHandler()
    mockAuthService.authenticate(*) returns Future(Map.empty)

    val url = "/v1/patients/P12345/episodes/E12345/test"
    val payload =
      """
        |{
        |  "uid": "P12345",
        |  "ahc_number": "E12345",
        |  "location_id": "LOC001",
        |  "last_name": "Doe",
        |  "dob": "1990-12-31",
        |  "gender": "MALE",
        |  "email": "john.doe@example.com",
        |  "mobile": 1234567890
        |}
        |""".stripMargin

    val response = server.httpPost(path = url,
                                   postBody = payload,
                                   headers = FakeValidApiKeyHeaders,
                                   andExpect = BadRequest
                                  )

  }

  test("POST /v1/patients/:patient_id/episodes/:episode_id/test with missing request id") {
    mockAuthService.tokenHandler returns DefaultAuthApiKeyHandler()
    mockAuthService.authenticate(*) returns Future(Map.empty)

    val url = "/v1/patients/P12345/episodes/E12345/test"

    val response = server.httpPost(path = url,
                                   postBody = FakeValidPayload,
                                   headers = Map("X-API-KEY" -> "api_key"),
                                   andExpect = BadRequest
                                  )

  }

  test("POST /v1/sessions/:session_id with a valid token and email claim returns 201 with a location header") {
    val validToken =
      "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI8RU1BSUw-IiwiZXhwIjoyMzQxNDQ5NDgzLCJuYmYiOjE3MTA3Mjk0ODMsImlhdCI6MTcxMDcyOTQ4MywianRpIjoiPFBBU1NXT1JEPiIsInNlc3Npb25faWQiOiI8UEFTU1dPUkQ-IiwiZW1haWwiOiI8RU1BSUw-In0.KZ8y3X8yT1zQM4Cdd-I2tSwXBH2mmyBplsDmucv0ULw"

    mockService.createSession(*[CreateSessionParams]) returns Future.value("session-url")

    val url = s"/v1/sessions/$FakeSessionId"
    val json =
      s"""
         |{
         |    "user_batch_code": "$FakeUserBatchCode",
         |    "country_code": "$FakeCountryCode"
         |}
         |""".stripMargin

    val response = server.httpPost(path = url,
                                   postBody = json,
                                   headers = Map("Authorization" -> validToken),
                                   andExpect = Created
                                  )

    response.headerMap("Location") shouldBe "session-url"
  }

  test("POST /v1/sessions/:session_id with a valid token but no email claim returns 400") {
    val tokenWithoutEmail =
      "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI8RU1BSUw-IiwiZXhwIjoyMzQxNDQ5NjE2LCJuYmYiOjE3MTA3Mjk2MTYsImlhdCI6MTcxMDcyOTYxNiwianRpIjoiPFBBU1NXT1JEPiIsInNlc3Npb25faWQiOiI8UEFTU1dPUkQ-In0._0ol3kB7zT-tnaHjcowFO2ChB-OgVFK1sVvnGX2qwl0"

    val url = s"/v1/sessions/$FakeSessionId"
    val json =
      s"""
         |{
         |    "user_batch_code": "$FakeUserBatchCode",
         |    "country_code": "$FakeCountryCode"
         |}
         |""".stripMargin

    server.httpPost(path = url,
                    postBody = json,
                    headers = Map("Authorization" -> tokenWithoutEmail),
                    andExpect = BadRequest
                   )
  }

  test("POST /v1/sessions/:session_id with a valid token but create session failed returns 400") {
    val validToken =
      "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI8RU1BSUw-IiwiZXhwIjoyMzQxNDQ5NDgzLCJuYmYiOjE3MTA3Mjk0ODMsImlhdCI6MTcxMDcyOTQ4MywianRpIjoiPFBBU1NXT1JEPiIsInNlc3Npb25faWQiOiI8UEFTU1dPUkQ-IiwiZW1haWwiOiI8RU1BSUw-In0.KZ8y3X8yT1zQM4Cdd-I2tSwXBH2mmyBplsDmucv0ULw"
    mockService.createSession(*[CreateSessionParams]) returns Future.exception(BizException("error-message"))

    val url = s"/v1/sessions/$FakeSessionId"
    val json =
      s"""
         |{
         |    "user_batch_code": "$FakeUserBatchCode",
         |    "country_code": "$FakeCountryCode"
         |}
         |""".stripMargin

    server.httpPost(path = url, postBody = json, headers = Map("Authorization" -> validToken), andExpect = BadRequest)
  }

  test("POST /v1/sessions/:session_id with an invalid token returns 401") {
    val invalidToken = "Bearer invalidToken"

    val url = s"/v1/sessions/$FakeSessionId"
    val json =
      s"""
         |{
         |    "user_batch_code": "$FakeUserBatchCode",
         |    "country_code": "$FakeCountryCode"
         |}
         |""".stripMargin

    server.httpPost(path = url,
                    postBody = json,
                    headers = Map("Authorization" -> invalidToken),
                    andExpect = Unauthorized
                   )
  }

  test("POST /v1/sessions/:session_id return 201 with a location header for empty auth header") {
    val _   = mockService.createUserAndSession(*[CreateUserSessionParams]) returns Future.value("fake-url")
    val url = s"/v1/sessions/$FakeSessionId"
    val json =
      s"""
         |{
         |    "user_batch_code": "$FakeUserBatchCode",
         |    "country_code": "$FakeCountryCode"
         |}
         |""".stripMargin

    val response = server.httpPost(path = url,
                                   postBody = json,
                                   headers = Map("Authorization" -> "no-bearer-token"),
                                   andExpect = Created
                                  )

    val captor = ArgCaptor[CreateUserSessionParams]

    val _ = mockService.createUserAndSession(captor) wasCalled once
    val _ = captor.hasCaptured(FakeCreateUserSessionParams)
    val _ = response.headerMap("Location") should equal("fake-url")
    val _ = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id return 201 with a location header") {
    val _   = mockService.createUserAndSession(*[CreateUserSessionParams]) returns Future.value("fake-url")
    val url = s"/v1/sessions/$FakeSessionId"
    val json =
      s"""
         |{
         |    "user_batch_code": "$FakeUserBatchCode",
         |    "country_code": "$FakeCountryCode"
         |}
         |""".stripMargin

    val response = server.httpPost(path = url, postBody = json, andExpect = Created)

    val captor = ArgCaptor[CreateUserSessionParams]

    val _ = mockService.createUserAndSession(captor) wasCalled once
    val _ = captor.hasCaptured(FakeCreateUserSessionParams)
    val _ = response.headerMap("Location") should equal("fake-url")
    val _ = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id create user and session failed return 400") {
    val _ =
      mockService.createUserAndSession(*[CreateUserSessionParams]) returns Future.exception(BizException("error-msg"))
    val url = s"/v1/sessions/$FakeSessionId"
    val json =
      s"""
         |{
         |    "user_batch_code": "$FakeUserBatchCode",
         |    "country_code": "$FakeCountryCode"
         |}
         |""".stripMargin

    val response = server.httpPost(path = url, postBody = json, andExpect = BadRequest)

    val captor = ArgCaptor[CreateUserSessionParams]

    val _ = mockService.createUserAndSession(captor) wasCalled once
    val _ = captor.hasCaptured(FakeCreateUserSessionParams)
    val _ = mockService wasNever calledAgain
  }

  test("PUT /v1/sessions/:session_id return a 204") {
    val _   = mockService.updateSession(*[UpdateSessionParams]) returns Future.Unit
    val url = s"/v1/sessions/$FakeSessionId"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail"
         |}
         |""".stripMargin

    val _ = server.httpPut(path = url, putBody = json, andExpect = NoContent)

    val captor = ArgCaptor[UpdateSessionParams]
    val _      = mockService.updateSession(captor) wasCalled once
    val _      = captor.hasCaptured(UpdateSessionParams(FakeSessionId, FakeValidEmail))
    val _      = mockService wasNever calledAgain
  }

  test("PUT /v1/sessions/:session_id return a 409") {
    val _ =
      mockService.updateSession(*[UpdateSessionParams]) returns Future.exception(UserExistsException("error message"))
    val url = s"/v1/sessions/$FakeSessionId"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail"
         |}
         |""".stripMargin

    val _ = server.httpPut(path = url, putBody = json, andExpect = Conflict)

    val captor = ArgCaptor[UpdateSessionParams]
    val _      = mockService.updateSession(captor) wasCalled once
    val _      = captor.hasCaptured(UpdateSessionParams(FakeSessionId, FakeValidEmail))
    val _      = mockService wasNever calledAgain
  }

  test("PUT /v1/sessions/:session_id return a 404") {
    val _ =
      mockService.updateSession(*[UpdateSessionParams]) returns Future.exception(
        UserNotFoundException("error message")
      )
    val url = s"/v1/sessions/$FakeSessionId"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail"
         |}
         |""".stripMargin

    val _ = server.httpPut(path = url, putBody = json, andExpect = NotFound)

    val captor = ArgCaptor[UpdateSessionParams]
    val _      = mockService.updateSession(captor) wasCalled once
    val _      = captor.hasCaptured(UpdateSessionParams(FakeSessionId, FakeValidEmail))
    val _      = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/verify return a 200 with jwt") {
    val _   = mockService.verifySession(*[VerifySessionParams]) returns Future.value("fake-jwt")
    val url = s"/v1/sessions/$FakeSessionId/verify"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "name": "$FakeName",
         |    "otp": "$FakeValidOtp"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = Ok)

    val captor = ArgCaptor[VerifySessionParams]
    val _      = mockService.verifySession(captor) wasCalled once
    val _ = captor.hasCaptured(
      FakeSessionParams
    )
    val _ = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/verify return a 410 OtpExpiredException") {
    val _ =
      mockService.verifySession(*[VerifySessionParams]) returns Future.exception(OtpExpiredException("error message"))
    val url = s"/v1/sessions/$FakeSessionId/verify"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "name": "$FakeName",
         |    "otp": "$FakeInvalidOtp"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = Gone)

    val captor = ArgCaptor[VerifySessionParams]
    val _      = mockService.verifySession(captor) wasCalled once
    val _ = captor.hasCaptured(
      FakeSessionParams.copy(otp = FakeInvalidOtp)
    )
    val _ = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/verify return a 401 OtpTriesExceededException") {
    val _ =
      mockService.verifySession(*[VerifySessionParams]) returns Future.exception(
        OtpTriesExceededException("error message")
      )
    val url = s"/v1/sessions/$FakeSessionId/verify"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "name": "$FakeName",
         |    "otp": "$FakeInvalidOtp"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = Unauthorized)

    val captor = ArgCaptor[VerifySessionParams]
    val _      = mockService.verifySession(captor) wasCalled once
    val _ = captor.hasCaptured(
      FakeSessionParams.copy(otp = FakeInvalidOtp)
    )
    val _ = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/verify return a 401 OtpMismatchException") {
    val _ =
      mockService.verifySession(*[VerifySessionParams]) returns Future.exception(OtpMismatchException("error message"))
    val url = s"/v1/sessions/$FakeSessionId/verify"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "name": "$FakeName",
         |    "otp": "$FakeInvalidOtp"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = Unauthorized)

    val captor = ArgCaptor[VerifySessionParams]
    val _      = mockService.verifySession(captor) wasCalled once
    val _ = captor.hasCaptured(
      FakeSessionParams.copy(otp = FakeInvalidOtp)
    )
    val _ = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/verify return a 404 SessionNotFoundException") {
    val _ =
      mockService.verifySession(*[VerifySessionParams]) returns Future.exception(
        SessionNotFoundException("error message")
      )
    val url = s"/v1/sessions/$FakeSessionId/verify"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "name": "$FakeName",
         |    "otp": "$FakeInvalidOtp"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = NotFound)

    val captor = ArgCaptor[VerifySessionParams]
    val _      = mockService.verifySession(captor) wasCalled once
    val _ = captor.hasCaptured(
      FakeSessionParams.copy(otp = FakeInvalidOtp)
    )
    val _ = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/verify return a 409") {
    val _ =
      mockService.verifySession(*[VerifySessionParams]) returns Future.exception(UserExistsException("error message"))
    val url = s"/v1/sessions/$FakeSessionId/verify"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "name": "$FakeName",
         |    "otp": "$FakeInvalidOtp"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = Conflict)

    val captor = ArgCaptor[VerifySessionParams]
    val _      = mockService.verifySession(captor) wasCalled once
    val _      = captor.hasCaptured(VerifySessionParams(FakeSessionId, FakeValidEmail, FakeInvalidOtp, FakeName))
    val _      = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/verify return a 404") {
    val _ =
      mockService.verifySession(*[VerifySessionParams]) returns Future.exception(
        UserNotFoundException("error message")
      )
    val url = s"/v1/sessions/$FakeSessionId/verify"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "name": "$FakeName",
         |    "otp": "$FakeInvalidOtp"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = NotFound)

    val captor = ArgCaptor[VerifySessionParams]
    val _      = mockService.verifySession(captor) wasCalled once
    val _      = captor.hasCaptured(VerifySessionParams(FakeSessionId, FakeValidEmail, FakeInvalidOtp, FakeName))
    val _      = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/login return a 204") {
    val _   = mockService.login(*[LoginParams]) returns Future.Unit
    val url = s"/v1/sessions/$FakeSessionId/login"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = NoContent)

    val captor = ArgCaptor[LoginParams]
    val _      = mockService.login(captor) wasCalled once
    val _      = captor.hasCaptured(LoginParams(FakeSessionId, FakeValidEmail))
    val _      = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/login return a 204 for user not found") {
    val _ =
      mockService.login(*[LoginParams]) returns Future.exception(
        UserNotFoundException("error message")
      )
    val url = s"/v1/sessions/$FakeSessionId/login"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = NoContent)

    val captor = ArgCaptor[LoginParams]
    val _      = mockService.login(captor) wasCalled once
    val _      = captor.hasCaptured(LoginParams(FakeSessionId, FakeValidEmail))
    val _      = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/login/verify return a 200 with jwt") {
    val _   = mockService.verifyLogin(*[VerifyLoginParams]) returns Future.value("fake-jwt")
    val url = s"/v1/sessions/$FakeSessionId/login/verify"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "otp": "$FakeValidOtp"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = Ok)

    val captor = ArgCaptor[VerifyLoginParams]
    val _      = mockService.verifyLogin(captor) wasCalled once
    val _      = captor.hasCaptured(VerifyLoginParams(FakeSessionId, FakeValidEmail, FakeValidOtp))
    val _      = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/login/verify return a 410 OtpExpiredException") {
    val _ = mockService.verifyLogin(*[VerifyLoginParams]) returns Future.exception(OtpExpiredException("error message"))
    val url = s"/v1/sessions/$FakeSessionId/login/verify"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "otp": "$FakeInvalidOtp"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = Gone)

    val captor = ArgCaptor[VerifyLoginParams]
    val _      = mockService.verifyLogin(captor) wasCalled once
    val _      = captor.hasCaptured(VerifyLoginParams(FakeSessionId, FakeValidEmail, FakeInvalidOtp))
    val _      = mockService wasNever calledAgain

  }

  test("POST /v1/sessions/:session_id/login/verify return a 401 OtpMismatchException") {
    val _ = mockService.verifyLogin(*[VerifyLoginParams]) returns Future.exception(OtpMismatchException("error message"))
    val url = s"/v1/sessions/$FakeSessionId/login/verify"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "otp": "$FakeInvalidOtp"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = Unauthorized)

    val captor = ArgCaptor[VerifyLoginParams]
    val _      = mockService.verifyLogin(captor) wasCalled once
    val _      = captor.hasCaptured(VerifyLoginParams(FakeSessionId, FakeValidEmail, FakeInvalidOtp))
    val _      = mockService wasNever calledAgain

  }

  test("POST /v1/sessions/:session_id/login/verify return a 401 OtpTriesExceededException") {
    val _ =
      mockService.verifyLogin(*[VerifyLoginParams]) returns Future.exception(OtpTriesExceededException("error message"))
    val url = s"/v1/sessions/$FakeSessionId/login/verify"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "otp": "$FakeInvalidOtp"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = Unauthorized)

    val captor = ArgCaptor[VerifyLoginParams]
    val _      = mockService.verifyLogin(captor) wasCalled once
    val _      = captor.hasCaptured(VerifyLoginParams(FakeSessionId, FakeValidEmail, FakeInvalidOtp))
    val _      = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/login/verify return a 404 SessionNotFoundException") {
    val _ =
      mockService.verifyLogin(*[VerifyLoginParams]) returns Future.exception(SessionNotFoundException("error message"))
    val url = s"/v1/sessions/$FakeSessionId/login/verify"

    val json =
      s"""
         |{
         |    "email": "$FakeValidEmail",
         |    "otp": "$FakeInvalidOtp"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = NotFound)

    val captor = ArgCaptor[VerifyLoginParams]
    val _      = mockService.verifyLogin(captor) wasCalled once
    val _      = captor.hasCaptured(VerifyLoginParams(FakeSessionId, FakeValidEmail, FakeInvalidOtp))
    val _      = mockService wasNever calledAgain

  }

  test("POST /v1/sessions/:session_id/result should return 204 No Content on successful report sending") {
    val _   = mockService.sendUserReport(*[SendUserReport]) returns Future.value(Response())
    val url = s"/v1/sessions/$FakeSessionId/result"
    val json =
      s"""
         |{
         |    "user_batch_code": "$FakeUserBatchCode"
         |}
         |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, andExpect = com.twitter.finagle.http.Status.NoContent)

    val captor = ArgCaptor[SendUserReport]
    val _      = mockService.sendUserReport(captor) wasCalled once
    val _      = captor.hasCaptured(SendUserReport(FakeSessionId, FakeUserBatchCode))
    val _      = mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/survey return a 201 for new user") {
    val url    = s"/v1/sessions/$FakeSessionId/survey"
    val captor = ArgCaptor[CreateUserSurveyParams]
    mockService.createUserSurvey(captor) returns Future.Unit

    val json =
      """
        |{
        |    "survey_items": [
        |    {
        |      "key": "option1",
        |      "value": "value1"
        |    }
        |  ]
        |}
        |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, headers = Map.empty, andExpect = Created)
    captor.value.username shouldBe FakeSessionId

    mockService.createUserSurvey(*[CreateUserSurveyParams]) wasCalled once
    mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/survey return a 201 for returning user") {
    val url    = s"/v1/sessions/$FakeSessionId/survey"
    val captor = ArgCaptor[CreateUserSurveyParams]
    mockService.createUserSurvey(captor) returns Future.Unit

    val json =
      """
        |{
        |    "survey_items": [
        |    {
        |      "key": "option1",
        |      "value": "value1"
        |    }
        |  ]
        |}
        |""".stripMargin

    val _ = server.httpPost(path = url, postBody = json, headers = FakeValidHeaders, andExpect = Created)
    captor.value.username shouldBe "test@example.com"

    mockService.createUserSurvey(*[CreateUserSurveyParams]) wasCalled once
    mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/survey fails when new user not found") {
    val url = s"/v1/sessions/$FakeSessionId/survey"

    val json =
      """
        |{
        |    "survey_items": [
        |    {
        |      "key": "option1",
        |      "value": "value1"
        |    }
        |  ]
        |}
        |""".stripMargin
    mockService.createUserSurvey(*[CreateUserSurveyParams]) returns Future.exception(
      UserNotFoundException("User not found")
    )

    val msg = server.httpPost(path = url, postBody = json, headers = Map.empty, andExpect = NotFound)
    msg.contentString shouldBe "{\"errors\":[\"User not found\"]}"

    mockService.createUserSurvey(*[CreateUserSurveyParams]) wasCalled once
    mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/survey fails when existing user not found") {
    val url = s"/v1/sessions/$FakeSessionId/survey"

    val json =
      """
        |{
        |    "survey_items": [
        |    {
        |      "key": "option1",
        |      "value": "value1"
        |    }
        |  ]
        |}
        |""".stripMargin
    mockService.createUserSurvey(*[CreateUserSurveyParams]) returns Future.exception(
      UserNotFoundException("User not found")
    )

    val msg = server.httpPost(path = url, postBody = json, headers = FakeValidHeaders, andExpect = NotFound)
    msg.contentString shouldBe "{\"errors\":[\"User not found\"]}"

    mockService.createUserSurvey(*[CreateUserSurveyParams]) wasCalled once
    mockService wasNever calledAgain
  }

  test("POST /v1/sessions/:session_id/survey with a valid token but no email claim returns 400") {
    val tokenWithoutEmail =
      "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI8RU1BSUw-IiwiZXhwIjoyMzQxNDQ5NjE2LCJuYmYiOjE3MTA3Mjk2MTYsImlhdCI6MTcxMDcyOTYxNiwianRpIjoiPFBBU1NXT1JEPiIsInNlc3Npb25faWQiOiI8UEFTU1dPUkQ-In0._0ol3kB7zT-tnaHjcowFO2ChB-OgVFK1sVvnGX2qwl0"

    val url = s"/v1/sessions/$FakeSessionId/survey"
    val json =
      """
        |{
        |    "survey_items": [
        |    {
        |      "key": "option1",
        |      "value": "value1"
        |    }
        |  ]
        |}
        |""".stripMargin

    server.httpPost(path = url,
                    postBody = json,
                    headers = Map("Authorization" -> tokenWithoutEmail),
                    andExpect = BadRequest
                   )
  }

  test("POST /v1/sessions/:session_id/survey with an invalid token returns 401") {
    val invalidToken = "Bearer invalidToken"

    val url = s"/v1/sessions/$FakeSessionId/survey"
    val json =
      """
        |{
        |    "survey_items": [
        |    {
        |      "key": "option1",
        |      "value": "value1"
        |    }
        |  ]
        |}
        |""".stripMargin

    server.httpPost(path = url,
                    postBody = json,
                    headers = Map("Authorization" -> invalidToken),
                    andExpect = Unauthorized
                   )
  }

  test("GET /v1/sessions/allow-new endpoint return the test creation allowance status") {
    val url = "/v1/sessions/allow-new"
    mockService.isNewSessionAllowedByUserName(*[CheckNewSessionAllowedParams]) returns Future.True
    mockService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val msg = server.httpGet(path = url, headers = FakeValidHeaders, andExpect = Ok)

    val expectedResponse = """{"is_allowed":true}""".stripMargin

    msg.contentString shouldBe expectedResponse

    mockService.isNewSessionAllowedByUserName(*[CheckNewSessionAllowedParams]) wasCalled once
    mockService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
  }

  test("GET /v1/sessions/latest endpoint return the latest test session information") {
    val url = "/v1/sessions/latest"
    mockService.getLatestCompletedSession(*[LatestCompletedSessionParams]) returns Future.value(
      LatestTestSession("this-is-a-fake-session-id", isScoreReady = true)
    )
    mockService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val msg = server.httpGet(path = url, headers = FakeValidHeaders, andExpect = Ok)

    val expectedResponse = """{"session_id":"this-is-a-fake-session-id","is_score_ready":true}""".stripMargin

    msg.contentString shouldBe expectedResponse
    mockService.getLatestCompletedSession(*[LatestCompletedSessionParams]) wasCalled once
    mockService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
  }

  test("GET /v1/sessions/latest endpoint return not found for SessionNotFoundException") {
    val url = "/v1/sessions/latest"
    mockService.getLatestCompletedSession(*[LatestCompletedSessionParams]) returns Future.exception(
      SessionNotFoundException("error-message")
    )
    mockService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

    val msg = server.httpGet(path = url, headers = FakeValidHeaders, andExpect = NotFound)

    val expectedResponse = "{\"errors\":[\"error-message\"]}"

    msg.contentString shouldBe expectedResponse
    mockService.getLatestCompletedSession(*[LatestCompletedSessionParams]) wasCalled once
    mockService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
  }

  test("POST /v1/logout endpoint successfully log out a user") {
    val url = "/v1/logout"
    mockService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))
    mockService.invalidateSessionOtp(*[String], *[String]) returns Future.Unit

    val msg = server.httpPost(path = url, headers = FakeValidHeaders, postBody = "", andExpect = Ok)

    msg.contentString shouldBe ""
    mockService.getSessionOtp(*[String], *[String]) wasCalled once
    mockService.invalidateSessionOtp(*[String], *[String]) wasCalled once
    mockService wasNever calledAgain
  }

}

private object SessionControllerTest {

  private final val FakeValidApiKeyHeaders = Map("request-id" -> "fake-request-id", "X-API-KEY" -> "fake-api-key ")

  private final val FakeInvalidApiKeyHeaders = Map("request-id" -> "fake-request-id")

  private final val FakeValidPayload =
    """
      |{
      |  "uid": "P12345",
      |  "ahc_number": "E12345",
      |  "location_id": "LOC001",
      |  "first_name": "John",
      |  "last_name": "Doe",
      |  "dob": "1990-12-31",
      |  "gender": "MALE",
      |  "email": "john.doe@example.com",
      |  "mobile": 1234567890
      |}
      |""".stripMargin

  private class TestServer extends HttpServer {

    override protected def configureHttp(router: HttpRouter): Unit = {
      router
        .filter[ExceptionMappingFilter[Request]]
        .exceptionMapper[AuthenticationExceptionMapper]
        .exceptionMapper[BizExceptionMapper]
        .add[SessionController]: Unit
    }

  }

}

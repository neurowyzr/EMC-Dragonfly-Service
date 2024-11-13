package com.neurowyzr.nw.dragon.service.api.filter

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.response.DefaultResponseBuilder.Instance.unauthorized
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.{
  FakeExpiredJwt, FakeLocalDateTimeFuture, FakeLocalDateTimePast, FakeValidJwt
}
import com.neurowyzr.nw.dragon.service.api.filters.JwtFilterService
import com.neurowyzr.nw.dragon.service.biz.SessionService
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeSessionOtp

import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class JwtFilterServiceTest
    extends AnyWordSpecLike with IdiomaticMockito with ArgumentMatchersSugar with Matchers
    with ResetMocksAfterEachTest {

  private val mockSessionService = mock[SessionService]

  "JwtFilterService" should {
    "return unauthorized if Authorization header is missing" in {
      val responseBuilder = mock[ResponseBuilder]
      val _               = responseBuilder.unauthorized(any[String]) returns unauthorized("Unauthorized")

      val service          = mock[Service[Request, Response]]
      val jwtFilterService = new JwtFilterService(responseBuilder, mockSessionService)

      val request = Request()

      val response = Await.result(jwtFilterService.apply(request, service))

      val _ = response.status shouldBe Status.Unauthorized
      val _ = response.contentString shouldBe "Unauthorized"
    }

    "call service if invalidationDate is empty" in {
      val responseBuilder = mock[ResponseBuilder]

      val serviceResponse = Response(Status.Ok)
      val service =
        new Service[Request, Response] {
          def apply(request: Request): Future[Response] = Future.value(serviceResponse)
        }
      val jwtFilterService = new JwtFilterService(responseBuilder, mockSessionService)

      val request = Request()
      val _       = request.headerMap.add("Authorization", "Bearer " + FakeValidJwt)
      val _       = mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

      val response = Await.result(jwtFilterService.apply(request, service))

      val _ = response shouldBe serviceResponse
    }

    "call service if session otp record is not found" in {
      val responseBuilder = mock[ResponseBuilder]

      val serviceResponse = Response(Status.Ok)
      val service =
        new Service[Request, Response] {
          def apply(request: Request): Future[Response] = Future.value(serviceResponse)
        }
      val jwtFilterService = new JwtFilterService(responseBuilder, mockSessionService)

      val request = Request()
      val _       = request.headerMap.add("Authorization", "Bearer " + FakeValidJwt)
      val _       = mockSessionService.getSessionOtp(*[String], *[String]) returns Future.None

      val response = Await.result(jwtFilterService.apply(request, service))

      val _ = response shouldBe serviceResponse
    }

    "call service if invalidationDate is after time now" in {
      val responseBuilder = mock[ResponseBuilder]

      val serviceResponse = Response(Status.Ok)
      val service =
        new Service[Request, Response] {
          def apply(request: Request): Future[Response] = Future.value(serviceResponse)
        }
      val jwtFilterService = new JwtFilterService(responseBuilder, mockSessionService)

      val request = Request()
      val _       = request.headerMap.add("Authorization", "Bearer " + FakeValidJwt)
      val _ =
        mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(
          Some(FakeSessionOtp.copy(maybeUtcInvalidatedAt = Some(FakeLocalDateTimeFuture)))
        )

      val response = Await.result(jwtFilterService.apply(request, service))

      val _ = response shouldBe serviceResponse
    }

    "return unauthorized if invalidationDate is before time now" in {
      val responseBuilder = mock[ResponseBuilder]
      val _               = responseBuilder.unauthorized(any[String]) returns unauthorized("Unauthorized")

      val serviceResponse = Response(Status.Ok)
      val service =
        new Service[Request, Response] {
          def apply(request: Request): Future[Response] = Future.value(serviceResponse)
        }
      val jwtFilterService = new JwtFilterService(responseBuilder, mockSessionService)

      val request = Request()
      val _       = request.headerMap.add("Authorization", "Bearer " + FakeValidJwt)
      val _ =
        mockSessionService.getSessionOtp(*[String], *[String]) returns Future.value(
          Some(FakeSessionOtp.copy(maybeUtcInvalidatedAt = Some(FakeLocalDateTimePast)))
        )

      val response = Await.result(jwtFilterService.apply(request, service))

      val _ = response.status shouldBe Status.Unauthorized
      val _ = response.contentString shouldBe "Unauthorized"
    }

    "return unauthorized if token is invalid" in {
      val responseBuilder = mock[ResponseBuilder]
      val _               = responseBuilder.unauthorized(any[String]) returns unauthorized("Invalid token")

      val service          = mock[Service[Request, Response]]
      val jwtFilterService = new JwtFilterService(responseBuilder, mockSessionService)

      val request = Request()
      val _       = request.headerMap.add("Authorization", "Bearer " + FakeExpiredJwt)

      val response = Await.result(jwtFilterService.apply(request, service))

      val _ = response.status shouldBe Status.Unauthorized
      val _ = response.contentString shouldBe "Invalid token"
    }
  }

}

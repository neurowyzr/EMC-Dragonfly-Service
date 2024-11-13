package com.neurowyzr.nw.dragon.service.api

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.api.FakeCustomerController.*

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.inject.Singleton

@Singleton
class FakeCustomerController extends Controller with Logging {

  post("/api/User/authenticate") { (request: CreateTokenRequest) =>
    val log =
      s"fake customer /authenticate request is ${request.toString} and headers are ${request.underlying.headerMap.toString()}"
    info(log)
    response.ok(
      VerifyAuthenticationResponse("new-auth-token", VendorCode, NoErrors)
    )
  }

  post("/v1/patients/:patient_id/episodes/:episode_id/LocationId/:location_id/status") {
    (request: TestSessionRequest) =>
      val log =
        s"fake customer /status request is ${request.toString} and headers are ${request.underlying.headerMap.toString()}"
      info(log)
      response.ok(TestSessionResponse(Ok, ReqAcknowledged))
  }

  post("/v1/patients/:patient_id/episodes/:episode_id/LocationId/:location_id/report") {
    (request: TestSessionResultRequest) =>
      val log =
        s"fake customer /report request is ${request.base64Pdf.get.length.toString} characters and headers are ${request.underlying.headerMap.toString()}"
      info(log)
      response.ok(TestSessionResponse(Ok, ReqAcknowledged))
  }

}

private object FakeCustomerController {

  private val Ok              = 204
  private val VendorCode      = "Neurowyzr_WebApi"
  private val NoErrors        = "No Error(s) found"
  private val ReqAcknowledged = "Request is acknowledged"

  final case class VerifyAuthenticationResponse(@JsonProperty("authToken") authToken: String,
                                                @JsonProperty("vendorCode") vendorCode: String,
                                                @JsonProperty("errorDetails") errorDetails: String
                                               )

  final case class TestSessionResponse(status: Int, description: String)

  private final case class CreateTokenRequest(
      @JsonProperty("Username") username: String,
      @JsonProperty("Password") password: String,
      underlying: Request
  )

  private final case class TestSessionRequest(
      status: Int,
      link: Option[String],
      underlying: Request
  )

  private final case class TestSessionResultRequest(
      @JsonProperty("Base64Pdf") base64Pdf: Option[String],
      underlying: Request
  )

}

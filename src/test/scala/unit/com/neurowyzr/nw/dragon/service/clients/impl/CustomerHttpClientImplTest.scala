package com.neurowyzr.nw.dragon.service.clients.impl

import java.util.Base64

import com.twitter.finagle.http.{Response, Status}
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.httpclient.test.InMemoryHttpService
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeCustomerServiceConfig
import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.CreateTestSessionArgs
import com.neurowyzr.nw.finatra.lib.clients.AlerterHttpClient

import org.mockito.scalatest.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CustomerHttpClientImplTest extends AnyFunSuite with MockitoSugar with Matchers {

  private val inMemoryHttpService = new InMemoryHttpService()
  private val testMapper          = (new ScalaObjectMapperModule).camelCaseObjectMapper
  private val testClient          = new HttpClient(httpService = inMemoryHttpService, mapper = testMapper)
  private val alerter             = mock[AlerterHttpClient]
  private val client              = new CustomerHttpClientImpl(FakeCustomerServiceConfig, testClient, alerter)

  private val params = CreateTestSessionArgs(
    requestId = "correlation-id",
    patientRef = "patient-ref",
    episodeRef = "episode-ref",
    locationId = "location-id"
  )

  test("notifyCreateSucceeded should authenticate and log the correct parameters, then return Future.Unit") {
    val authRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
                                                   FakeCustomerServiceConfig.password
                                                  )
    )

    val authResponse = Response(Status.Ok)
    authResponse.setContentString("""{"authToken": "valid-token", "vendorCode": "vendor", "errorDetails": ""}""")

    val authPath = s"/api/User/authenticate"
    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse)

    val notifyRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.StatusRequest(1, Some("http://example.com/magic-link"))
    )

    val notifyResponse = Response(Status.Ok)

    val notifyPath =
      s"/v1/patients/${params.patientRef}/episodes/${params.episodeRef}/LocationId/${params.locationId}/status"

    inMemoryHttpService.mockPost(notifyPath, notifyRequestBody, andReturn = notifyResponse)

    val magicLink = "http://example.com/magic-link"

    val result: Future[Unit] = client.notifyCreateSucceeded(params, magicLink)

    Await.result(result) shouldBe ()
  }

  test("notifyCreateSucceeded should handle authentication failure") {
    val authRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
                                                   FakeCustomerServiceConfig.password
                                                  )
    )

    val authResponse = Response(Status.Unauthorized)
    authResponse.setContentString("""{"error": "Invalid credentials"}""")

    val authPath = s"/api/User/authenticate"
    inMemoryHttpService.clear()
    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse, sticky = true)

    val magicLink = "http://example.com/magic-link"

    val result: Future[Unit] = client.notifyCreateSucceeded(params, magicLink)

    val thrown = intercept[BizException] {
      Await.result(result)
    }

    thrown.getMessage should include("Authentication failed with status: Status(401)")
  }

  test("notifyCreateSucceeded should handle authentication response parsing failure") {
    val authRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
                                                   FakeCustomerServiceConfig.password
                                                  )
    )

    val authResponse = Response(Status.Ok)
    authResponse.setContentString("""{"authToken": 12345}""")

    val authPath = s"/api/User/authenticate"
    inMemoryHttpService.clear()
    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse, sticky = true)

    val magicLink = "http://example.com/magic-link"

    val result: Future[Unit] = client.notifyCreateSucceeded(params, magicLink)

    val thrown = intercept[BizException] {
      Await.result(result)
    }

    thrown.getMessage should include("Failed to parse authentication response")
  }

  test("notifyCreateSucceeded should handle notify request failure") {
    val authRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
                                                   FakeCustomerServiceConfig.password
                                                  )
    )

    val authResponse = Response(Status.Ok)
    authResponse.setContentString("""{"authToken": "valid-token", "vendorCode": "vendor", "errorDetails": ""}""")

    val authPath = s"/api/User/authenticate"
    inMemoryHttpService.clear()
    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse, sticky = true)

    val notifyRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.StatusRequest(1, Some("http://example.com/magic-link"))
    )

    val notifyResponse = Response(Status.InternalServerError)
    notifyResponse.setContentString("""{"error": "Internal Server Error"}""")

    val notifyPath =
      s"/v1/patients/${params.patientRef}/episodes/${params.episodeRef}/LocationId/${params.locationId}/status"
    inMemoryHttpService.mockPost(notifyPath, notifyRequestBody, andReturn = notifyResponse, sticky = true)

    val magicLink = "http://example.com/magic-link"

    val result: Future[Unit] = client.notifyCreateSucceeded(params, magicLink)

    val thrown = intercept[BizException] {
      Await.result(result)
    }

    thrown.getMessage should include("Failed to notify create succeeded")
  }

//  test("notifyCreateSucceeded should handle alert notification failure") {
//    val authRequestBody = testMapper.writeValueAsString(
//      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
//                                                   FakeCustomerServiceConfig.password
//                                                  )
//    )
//
//    val authResponse = Response(Status.Ok)
//    authResponse.setContentString("""{"authToken": "valid-token", "vendorCode": "vendor", "errorDetails": ""}""")
//
//    val authPath = s"/api/User/authenticate"
//    inMemoryHttpService.clear()
//    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse)
//
//    val notifyRequestBody = testMapper.writeValueAsString(
//      CustomerHttpClientImpl.StatusRequest(1, Some("http://example.com/magic-link"))
//    )
//
//    val notifyResponse = Response(Status.InternalServerError)
//    notifyResponse.setContentString("""{"error": "Internal Server Error"}""")
//
//    val notifyPath =
//      s"/v1/patients/${params.patientRef}/episodes/${params.episodeRef}/LocationId/${params.locationId}/status"
//
//    inMemoryHttpService.mockPost(notifyPath, notifyRequestBody, andReturn = notifyResponse, sticky = true)
//
//    val magicLink = "http://example.com/magic-link"
//
//    when(
//      alerter.notify(
//        eqTo("[ERROR] Failed to notify create succeeded"),
//        any[String]
//      )
//    ).thenReturn(Future.exception(new RuntimeException("Alert notification failed")))
//
//    val result: Future[Unit] = client.notifyCreateSucceeded(params, magicLink)
//
//    val thrown = intercept[BizException] {
//      Await.result(result)
//    }
//  }

  test("notifyCreateFailed should authenticate and log the correct parameters, then return Future.Unit") {
    val authRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
                                                   FakeCustomerServiceConfig.password
                                                  )
    )

    val authResponse = Response(Status.Ok)
    authResponse.setContentString("""{"authToken": "valid-token", "vendorCode": "vendor", "errorDetails": ""}""")

    val authPath = s"/api/User/authenticate"
    inMemoryHttpService.clear()
    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse)

    val notifyRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.StatusRequest(0, None)
    )

    val notifyResponse = Response(Status.Ok)

    val notifyPath =
      s"/v1/patients/${params.patientRef}/episodes/${params.episodeRef}/LocationId/${params.locationId}/status"

    inMemoryHttpService.mockPost(notifyPath, notifyRequestBody, andReturn = notifyResponse)

    val result: Future[Unit] = client.notifyCreateFailed(params, 0)

    Await.result(result) shouldBe ()
  }

  test("notifyCreateFailed should handle authentication failure") {
    val authRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
                                                   FakeCustomerServiceConfig.password
                                                  )
    )

    val authResponse = Response(Status.Unauthorized)
    authResponse.setContentString("""{"error": "Invalid credentials"}""")

    val authPath = s"/api/User/authenticate"
    inMemoryHttpService.clear()
    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse, sticky = true)

    val result: Future[Unit] = client.notifyCreateFailed(params, 0)

    val thrown = intercept[BizException] {
      Await.result(result)
    }

    thrown.getMessage should include("Authentication failed with status: Status(401)")
  }

  test("notifyCreateFailed should handle authentication response parsing failure") {
    val authRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
                                                   FakeCustomerServiceConfig.password
                                                  )
    )

    val authResponse = Response(Status.Ok)
    authResponse.setContentString("""{"authToken": 12345}""")

    val authPath = s"/api/User/authenticate"
    inMemoryHttpService.clear()
    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse, sticky = true)

    val result: Future[Unit] = client.notifyCreateFailed(params, 0)

    val thrown = intercept[BizException] {
      Await.result(result)
    }

    thrown.getMessage should include("Failed to parse authentication response")
  }

  test("notifyCreateFailed should handle notify request failure") {
    val authRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
                                                   FakeCustomerServiceConfig.password
                                                  )
    )

    val authResponse = Response(Status.Ok)
    authResponse.setContentString("""{"authToken": "valid-token", "vendorCode": "vendor", "errorDetails": ""}""")

    val authPath = s"/api/User/authenticate"
    inMemoryHttpService.clear()
    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse, sticky = true)

    val notifyRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.StatusRequest(0, None)
    )

    val notifyResponse = Response(Status.InternalServerError)
    notifyResponse.setContentString("""{"error": "Internal Server Error"}""")

    val notifyPath =
      s"/v1/patients/${params.patientRef}/episodes/${params.episodeRef}/LocationId/${params.locationId}/status"

    inMemoryHttpService.mockPost(notifyPath, notifyRequestBody, andReturn = notifyResponse, sticky = true)

    val result: Future[Unit] = client.notifyCreateFailed(params, 0)

    val thrown = intercept[BizException] {
      Await.result(result)
    }

    thrown.getMessage should include("Failed to notify create failed")
  }

//  test("notifyCreateFailed should handle alert notification failure") {
//    val authRequestBody = testMapper.writeValueAsString(
//      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
//                                                   FakeCustomerServiceConfig.password
//                                                  )
//    )
//
//    val authResponse = Response(Status.Ok)
//    authResponse.setContentString("""{"authToken": "valid-token", "vendorCode": "vendor", "errorDetails": ""}""")
//
//    val authPath = s"/api/User/authenticate"
//    inMemoryHttpService.clear()
//    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse)
//
//    val notifyRequestBody = testMapper.writeValueAsString(
//      CustomerHttpClientImpl.StatusRequest(0, None)
//    )
//
//    val notifyResponse = Response(Status.InternalServerError)
//    notifyResponse.setContentString("""{"error": "Internal Server Error"}""")
//
//    val notifyPath =
//      s"/v1/patients/${params.patientRef}/episodes/${params.episodeRef}/LocationId/${params.locationId}/status"
//
//    inMemoryHttpService.mockPost(notifyPath, notifyRequestBody, andReturn = notifyResponse, sticky = true)
//
//    when(
//      alerter.notify(
//        eqTo("[ERROR] Failed to notify create failed"),
//        any[String]
//      )
//    ).thenReturn(Future.exception(new RuntimeException("Alert notification failed")))
//
//    val result: Future[Unit] = client.notifyCreateFailed(params, 0)
//
//    val thrown = intercept[BizException] {
//      Await.result(result)
//    }
//  }

  test("uploadReport should authenticate and upload report successfully") {
    val authRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
                                                   FakeCustomerServiceConfig.password
                                                  )
    )

    val authResponse = Response(Status.Ok)
    authResponse.setContentString("""{"authToken": "valid-token", "vendorCode": "vendor", "errorDetails": ""}""")

    val authPath = s"/api/User/authenticate"
    inMemoryHttpService.clear()
    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse)

    val report            = Array[Byte](1, 2, 3, 4, 5)
    val base64Report      = Base64.getEncoder.encodeToString(report)
    val uploadRequestBody = testMapper.writeValueAsString(CustomerHttpClientImpl.UploadReportRequest(base64Report))
    val uploadResponse    = Response(Status.Ok)
    val uploadPath =
      s"/v1/patients/${params.patientRef}/episodes/${params.episodeRef}/LocationId/${params.locationId}/report"

    inMemoryHttpService.mockPost(uploadPath, uploadRequestBody, andReturn = uploadResponse, sticky = true)

    val result: Future[Unit] = client.uploadReport(params, report)

    Await.result(result) shouldBe ()
  }

  test("uploadReport should handle authentication failure") {
    val authRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
                                                   FakeCustomerServiceConfig.password
                                                  )
    )

    val authResponse = Response(Status.Unauthorized)
    authResponse.setContentString("""{"error": "Invalid credentials"}""")

    val authPath = s"/api/User/authenticate"
    inMemoryHttpService.clear()
    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse, sticky = true)

    val report = Array[Byte](1, 2, 3, 4, 5)

    val result: Future[Unit] = client.uploadReport(params, report)

    val thrown = intercept[BizException] {
      Await.result(result)
    }

    thrown.getMessage should include("Authentication failed with status: Status(401)")
  }

  test("uploadReport should handle notify request failure") {
    val authRequestBody = testMapper.writeValueAsString(
      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
                                                   FakeCustomerServiceConfig.password
                                                  )
    )

    val authResponse = Response(Status.Ok)
    authResponse.setContentString("""{"authToken": "valid-token", "vendorCode": "vendor", "errorDetails": ""}""")

    val authPath = s"/api/User/authenticate"
    inMemoryHttpService.clear()
    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse, sticky = true)

    val report            = Array[Byte](1, 2, 3, 4, 5)
    val base64Report      = Base64.getEncoder.encodeToString(report)
    val uploadRequestBody = testMapper.writeValueAsString(CustomerHttpClientImpl.UploadReportRequest(base64Report))
    val uploadResponse    = Response(Status.InternalServerError)
    val uploadPath =
      s"/v1/patients/${params.patientRef}/episodes/${params.episodeRef}/LocationId/${params.locationId}/report"

    inMemoryHttpService.mockPost(uploadPath, uploadRequestBody, andReturn = uploadResponse, sticky = true)

    val result: Future[Unit] = client.uploadReport(params, report)

    val thrown = intercept[BizException] {
      Await.result(result)
    }

    thrown.getMessage should include("Failed to upload report")
  }

//  test("uploadReport should handle alert notification failure") {
//    val authRequestBody = testMapper.writeValueAsString(
//      CustomerHttpClientImpl.AuthenticationRequest(FakeCustomerServiceConfig.username,
//                                                   FakeCustomerServiceConfig.password
//                                                  )
//    )
//
//    val authResponse = Response(Status.Ok)
//    authResponse.setContentString("""{"authToken": "valid-token", "vendorCode": "vendor", "errorDetails": ""}""")
//
//    val authPath = s"/api/User/authenticate"
//    inMemoryHttpService.clear()
//    inMemoryHttpService.mockPost(authPath, authRequestBody, andReturn = authResponse, sticky = true)
//
//    val report            = Array[Byte](1, 2, 3, 4, 5)
//    val base64Report      = Base64.getEncoder.encodeToString(report)
//    val uploadRequestBody = testMapper.writeValueAsString(CustomerHttpClientImpl.UploadReportRequest(base64Report))
//    val uploadResponse    = Response(Status.InternalServerError)
//    val uploadPath =
//      s"/v1/patients/${params.patientRef}/episodes/${params.episodeRef}/LocationId/${params.locationId}/report"
//
//    inMemoryHttpService.mockPost(uploadPath, uploadRequestBody, andReturn = uploadResponse, sticky = true)
//
//    when(
//      alerter.notify(
//        eqTo("[ERROR] Failed to upload report"),
//        any[String]
//      )
//    ).thenReturn(Future.exception(new RuntimeException("Alert notification failed")))
//
//    val result: Future[Unit] = client.uploadReport(params, report)
//
//    val thrown = intercept[BizException] {
//      Await.result(result)
//    }
//  }

}

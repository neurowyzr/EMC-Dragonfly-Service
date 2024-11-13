package com.neurowyzr.nw.dragon.service.clients.impl

import java.util.Base64
import javax.inject.Inject

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finagle.Backoff
import com.twitter.finagle.http.{Request, Status}
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finatra.http.request.RequestBuilder
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.util.{Await, Future, Return, Throw, Try}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.CreateTestSessionArgs
import com.neurowyzr.nw.dragon.service.cfg.Models.CustomerServiceConfig
import com.neurowyzr.nw.dragon.service.clients.CustomerHttpClient
import com.neurowyzr.nw.dragon.service.clients.impl.CustomerHttpClientImpl.{
  AuthenticationRequest, AuthenticationResponse, StatusRequest, UploadReportRequest
}
import com.neurowyzr.nw.finatra.lib.clients.AlerterHttpClient

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.inject.Singleton

@Singleton
class CustomerHttpClientImpl @Inject() (
    customerServiceConfig: CustomerServiceConfig,
    httpClient: HttpClient,
    alerter: AlerterHttpClient
) extends CustomerHttpClient with Logging {

  private val mapper = (new ScalaObjectMapperModule).camelCaseObjectMapper

  def httpRetryPolicy: RetryPolicy[(Try[Unit], Int)] =
    RetryPolicy.backoff(
      Backoff.exponentialJittered(customerServiceConfig.httpMinRetryDelay.second,
                                  customerServiceConfig.httpMaxRetryDelay.second
                                 )
    ) { case (Throw(_), retries) => retries < customerServiceConfig.httpMaxRetries }

  def httpAuthenticateRetryPolicy: RetryPolicy[(Try[String], Int)] =
    RetryPolicy.backoff(
      Backoff.exponentialJittered(customerServiceConfig.httpMinRetryDelay.second,
                                  customerServiceConfig.httpMaxRetryDelay.second
                                 )
    ) { case (Throw(_), retries) => retries < customerServiceConfig.httpMaxRetries }

  override def notifyCreateSucceeded(args: CreateTestSessionArgs, magicLink: String): Future[Unit] = {
    info(
      s"notifyCreateSucceeded called with patientRef: ${args.patientRef}, episodeRef: ${args.episodeRef}, " +
        s"locationId: ${args.locationId}, correlationId: ${args.requestId}"
    )

    authenticate(args.requestId).flatMap { token =>
      val request = createRequest(args, token, StatusRequest(1, Some(magicLink)))
      info(request.toString() + " with headers: " + request.headerMap.mkString(", "))
      def retryableOperation(): Try[Unit] = {
        val output = httpClient.execute(request).flatMap { response =>
          info(s"got response from apollo for /status API: ${response.contentString}, for requestId: ${args.requestId}")
          if (response.status == Status.Ok) {
            info(
              s"Successfully notified create succeeded for patientRef: ${args.patientRef}, for requestId: ${args.requestId}"
            )
            Future.Unit
          } else {
            val message =
              s"Failed to notify create succeeded for patientRef: ${args.patientRef}, " +
                s"episodeRef: ${args.episodeRef}, locationId: ${args.locationId}, correlationId: ${args.requestId}. " +
                s"Status: ${response.status.toString}, Response: ${response.contentString}"
            error(message)
            Future.exception(BizException(message))
          }
        }
        Try {
          Await.result(output)
        }
      }
      val tried = new Retryable("notifyCreateSucceeded operation", logger).using(retryableOperation, httpRetryPolicy)
      Future.const(tried)
    }
  }

  private def authenticate(requestId: String): Future[String] = {
    val authUrl     = "/api/User/authenticate"
    val requestBody = AuthenticationRequest(customerServiceConfig.username, customerServiceConfig.password)
    val request = RequestBuilder
      .post(authUrl)
      .body(mapper.writeValueAsString(requestBody))
      .header("Content-Type", "application/json")

    info(request.toString() + " with headers: " + request.headerMap.mkString(", ") + s", requestId: $requestId")
    def retryableOperation(): Try[String] = {
      val token = httpClient.execute(request).flatMap { response =>
        info(s"got response from apollo for /authenticate API: ${response.contentString}, requestId: $requestId")
        if (response.status == Status.Ok) {
          Future.const {
            Try(mapper.parse[AuthenticationResponse](response.contentString)) match {
              case Return(authResponse) => Return(authResponse.authToken)
              case Throw(e) =>
                error(s"Unable to parse authenticate response: ${response.contentString}", e)
                Throw(BizException(s"Failed to parse authentication response : ${e.getMessage}"))
            }
          }
        } else {
          val message =
            s"Failed to authenticate : Status: ${response.status.toString}, Response: ${response.contentString}"
          error(message)
          Future.exception(
            BizException(
              s"Authentication failed with status: ${response.status.toString}"
            )
          )
        }
      }
      Try {
        Await.result(token)
      }
    }
    val tried = new Retryable("authenticate operation", logger).using(retryableOperation, httpAuthenticateRetryPolicy)
    Future.const(tried)
  }

  private def createRequest(args: CreateTestSessionArgs, token: String, statusRequest: StatusRequest): Request = {
    val notifyUrl = s"/v1/patients/${args.patientRef}/episodes/${args.episodeRef}/LocationId/${args.locationId}/status"

    RequestBuilder
      .post(notifyUrl)
      .body(mapper.writeValueAsString(statusRequest))
      .header("Content-Type", "application/json")
      .header("Authorization", s"Bearer $token")
      .header("Correlation-Id", args.requestId)

  }

  override def notifyCreateFailed(args: CreateTestSessionArgs, statusCode: Int): Future[Unit] = {
    info(
      s"notifyCreateFailed called with patientRef: ${args.patientRef}, episodeRef: ${args.episodeRef}, " +
        s"locationId: ${args.locationId}, correlationId: ${args.requestId}"
    )
    authenticate(args.requestId).flatMap { token =>
      val request = createRequest(args, token, StatusRequest(statusCode, None))

      info(request.toString() + " with headers: " + request.headerMap.mkString(", "))
      def retryableOperation(): Try[Unit] = {
        val output = httpClient.execute(request).flatMap { response =>
          info(s"got response from apollo for /status API: ${response.contentString}, for requestId: ${args.requestId}")
          if (response.status == Status.Ok) {
            info(
              s"Successfully notified create failed for patientRef: ${args.patientRef}," +
                s" for requestId: ${args.requestId}"
            )
            Future.Unit
          } else {
            val message =
              s"Failed to notify create failed for patientRef: ${args.patientRef}, " +
                s"episodeRef: ${args.episodeRef}, locationId: ${args.locationId}, correlationId: ${args.requestId}. " +
                s"Status: ${response.status.toString}, Response: ${response.contentString}"
            error(message)
            Future.exception(BizException(message))
          }
        }
        Try {
          Await.result(output)
        }
      }
      val tried = new Retryable("notifyCreateFailed operation", logger).using(retryableOperation, httpRetryPolicy)
      Future.const(tried)
    }
  }

  override def uploadReport(args: CreateTestSessionArgs, report: Array[Byte]): Future[Unit] = {
    info(
      s"uploadReport called with patientRef: ${args.patientRef}, episodeRef: ${args.episodeRef}, " +
        s"locationId: ${args.locationId}, report size is : ${report.size.toString}, for requestId: ${args.requestId}"
    )
    val base64Report = Base64.getEncoder.encodeToString(report)
    authenticate(args.requestId).flatMap { token =>
      val uploadUrl = s"/v1/patients/${args.patientRef}/episodes/${args.episodeRef}/LocationId/${args.locationId}/report"
      val requestBody = UploadReportRequest(base64Report)
      val request = RequestBuilder
        .post(uploadUrl)
        .body(mapper.writeValueAsString(requestBody))
        .header("Content-Type", "application/json")
        .header("Authorization", s"Bearer $token")
        .header("Correlation-Id", args.requestId)
      info(request.toString() + " with headers: " + request.headerMap.mkString(", "))
      def retryableOperation(): Try[Unit] = {
        val output = httpClient.execute(request).flatMap { response =>
          info(s"got response from apollo for /report API: ${response.contentString}, for requestId: ${args.requestId}")
          if (response.status == Status.Ok) {
            info(s"Successfully uploaded report for patientRef: ${args.patientRef}, for requestId: ${args.requestId}")
            Future.Unit
          } else {
            val message =
              s"Failed to upload report for patientRef: ${args.patientRef}, " +
                s"episodeRef: ${args.episodeRef}, locationId: ${args.locationId}, correlationId: ${args.requestId}. " +
                s"Status: ${response.status.toString}, Response: ${response.contentString}"
            error(message)
            Future.exception(BizException(message))
          }
        }
        Try {
          Await.result(output)
        }
      }
      val tried = new Retryable("uploadReport operation", logger).using(retryableOperation, httpRetryPolicy)
      Future.const(tried)
    }
  }

}

object CustomerHttpClientImpl {

  final case class AuthenticationRequest(@JsonProperty("Username") username: String,
                                         @JsonProperty("Password") password: String
                                        )

  final case class AuthenticationResponse(authToken: String, vendorCode: String, errorDetails: String)

  final case class StatusRequest(status: Int, link: Option[String])

  final case class UploadReportRequest(@JsonProperty("Base64Pdf") base64Str: String)
}

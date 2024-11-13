package com.neurowyzr.nw.dragon.service.api

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject

import com.twitter.finagle.http.Fields.Authorization
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.annotations.RouteParam
import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.api.SessionController.*
import com.neurowyzr.nw.dragon.service.api.filters.JwtFilterService
import com.neurowyzr.nw.dragon.service.biz.SessionService
import com.neurowyzr.nw.dragon.service.biz.exceptions.*
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.{
  CheckNewSessionAllowedParams, EnqueueTestSessionParams, LatestCompletedSessionParams
}
import com.neurowyzr.nw.dragon.service.di.validators.MatchesSet
import com.neurowyzr.nw.dragon.service.mq.QueueConsumer
import com.neurowyzr.nw.dragon.service.utils.context.JwtUtil
import com.neurowyzr.nw.dragon.service.utils.context.JwtUtil.extractClaims
import com.neurowyzr.nw.dragon.service as root
import com.neurowyzr.nw.finatra.lib.api.filters.ApiKeyBasedAuthFilter

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.inject.Singleton
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.TransformationOps
import jakarta.validation.constraints.{Email, NotBlank, NotNull, Past, Pattern, Size}

@Singleton
class SessionController @Inject() (service: SessionService) extends Controller {

  filter[ApiKeyBasedAuthFilter] {
    post("/v1/patients/:patient_id/episodes/:episode_id/test") { (request: EnqueueTestSessionRequest) =>
      import Implicits.EnqueueTestSessionRequestToParamsTransformer

      extractRequestId(request) match {
        case "" => Future.exception(BizException("Missing/invalid request-id header."))
        case _ =>
          service.enqueueTestSession(request.into[EnqueueTestSessionParams].transform).map { resultString =>
            response.ok(resultString)
          }
      }

    }
  }

  post("/v1/sessions/:session_id") { (request: CreateSessionRequest) =>
    extractJwtToken(request.underlying) match {
      case Some(token) =>
        if (JwtUtil.isTokenValid(token)) {
          extractClaims(token).get("email") match {
            case Some(emailClaim) =>
              val email = emailClaim.asString()
              val params = root.biz.models.SessionModels
                .CreateSessionParams(request.sessionId, request.userBatchCode, email)
              service.createSession(params).map { sessionUrl =>
                response.created.location(sessionUrl)
              }
            case None =>
              warn("Email claim does not exist")
              response.badRequest("Email claim does not exist")
          }
        } else {
          response.unauthorized("Token is invalid.").toFuture
        }
      case None =>
        val params = request.into[root.biz.models.SessionModels.CreateUserSessionParams].transform
        service.createUserAndSession(params).map { sessionUrl =>
          response.created.location(sessionUrl)
        }
    }
  }

  put("/v1/sessions/:session_id") { (request: UpdateSessionRequest) =>
    val params = request.into[root.biz.models.SessionModels.UpdateSessionParams].transform

    service.updateSession(params).map(_ => response.noContent).rescue {
      case e: UserExistsException   => Future.value(response.conflict(ErrorMsg(e.getMessage)))
      case e: UserNotFoundException => Future.value(response.notFound(ErrorMsg(e.getMessage)))
    }
  }

  post("/v1/sessions/:session_id/verify") { (request: VerifySessionRequest) =>
    val params = request.into[root.biz.models.SessionModels.VerifySessionParams].transform

    service.verifySession(params).map(jwt => response.ok(VerifySessionResponse(jwt))).rescue {
      case e: OtpExpiredException       => Future.value(response.gone(ErrorMsg(e.getMessage)))
      case e: OtpTriesExceededException => Future.value(response.unauthorized(ErrorMsg(e.getMessage)))
      case e: OtpMismatchException      => Future.value(response.unauthorized(ErrorMsg(e.getMessage)))
      case e: SessionNotFoundException  => Future.value(response.notFound(ErrorMsg(e.getMessage)))
      case e: UserExistsException       => Future.value(response.conflict(ErrorMsg(e.getMessage)))
      case e: UserNotFoundException     => Future.value(response.notFound(ErrorMsg(e.getMessage)))
    }
  }

  post("/v1/sessions/:login_session_id/login") { (request: LoginRequest) =>
    val params = request.into[root.biz.models.SessionModels.LoginParams].transform

    service.login(params).map(_ => response.noContent).rescue { case _: UserNotFoundException =>
      Future.value(response.noContent)
    }
  }

  post("/v1/sessions/:login_session_id/login/verify") { (request: LoginVerifyRequest) =>
    val params = request.into[root.biz.models.SessionModels.VerifyLoginParams].transform

    service.verifyLogin(params).map(jwt => response.ok(VerifySessionResponse(jwt))).rescue {
      case e: OtpExpiredException       => Future.value(response.gone(ErrorMsg(e.getMessage)))
      case e: OtpTriesExceededException => Future.value(response.unauthorized(ErrorMsg(e.getMessage)))
      case e: OtpMismatchException      => Future.value(response.unauthorized(ErrorMsg(e.getMessage)))
      case e: SessionNotFoundException  => Future.value(response.notFound(ErrorMsg(e.getMessage)))
    }
  }

  post("/v1/sessions/:session_id/result") { (request: SessionResultRequest) =>
    val params = request.into[root.biz.models.SessionModels.SendUserReport].transform

    service.sendUserReport(params).map(_ => response.noContent)
  }

  post("/v1/sessions/:session_id/survey") { (request: CreateSurveyRequest) =>
    extractJwtToken(request.underlying) match {
      case Some(token) =>
        if (JwtUtil.isTokenValid(token)) {
          extractClaims(token).get("email") match {
            case Some(emailClaim) =>
              val email = emailClaim.asString()
              val params =
                request
                  .into[root.biz.models.UserSurveyModels.CreateUserSurveyParams]
                  .withFieldConst(_.username, email)
                  .transform
              service.createUserSurvey(params).map(_ => response.created).rescue { case e: UserNotFoundException =>
                Future.value(response.notFound(ErrorMsg(e.getMessage)))
              }
            case None => response.badRequest("Email claim does not exist")
          }
        } else {
          response.unauthorized("Token is invalid.").toFuture
        }
      case None =>
        val params =
          request
            .into[root.biz.models.UserSurveyModels.CreateUserSurveyParams]
            .withFieldConst(_.username, request.sessionId)
            .transform
        service.createUserSurvey(params).map(_ => response.created).rescue { case e: UserNotFoundException =>
          Future.value(response.notFound(ErrorMsg(e.getMessage)))
        }
    }
  }

  filter[JwtFilterService].get("/v1/sessions/allow-new") { (request: Request) =>
    extractJwtToken(request) match {
      case Some(token) =>
        service
          .isNewSessionAllowedByUserName(CheckNewSessionAllowedParams(extractClaims(token)("email").asString()))
          .map(res => response.ok(AllowNewSessionResponse(res)))
    }
  }

  filter[JwtFilterService].get("/v1/sessions/latest") { (request: Request) =>
    extractJwtToken(request) match {
      case Some(jwt) =>
        JwtUtil.extractClaims(jwt).get("email") match {
          case Some(email) =>
            service
              .getLatestCompletedSession(LatestCompletedSessionParams(email.asString()))
              .flatMap { latestTestSession =>
                val responseBody = latestTestSession.into[LatestTestSessionResponse].transform
                Future.value(response.ok(responseBody))
              }
              .rescue { case e: SessionNotFoundException => Future.value(response.notFound(ErrorMsg(e.getMessage))) }
        }
    }
  }

  filter[JwtFilterService].post("/v1/logout") { (request: Request) =>
    extractJwtToken(request) match {
      case Some(jwtToken) =>
        val sessionId = JwtUtil.extractClaims(jwtToken)("session_id").asString()
        val email     = JwtUtil.extractClaims(jwtToken)("email").asString()
        service.invalidateSessionOtp(sessionId, email).flatMap { _ =>
          Future.value(response.ok)
        }
    }
  }

}

private object SessionController {
  // $COVERAGE-OFF$
  private final val EmailRegex       = "^[A-Z0-9._%-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$"
  private final val ClientEmailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
  private final val EmailMin         = 5
  private final val EmailMax         = 100
  private final val OtpMin           = 6
  private final val OtpMax           = 6
  private final val UserBatchCodeMin = 6
  private final val UserBatchCodeMax = 6
  // $COVERAGE-ON$

  private def extractJwtToken(request: Request): Option[String] = {
    request.headerMap.get(Authorization).flatMap { headerValue =>
      val bearerTokenPrefix = "Bearer "
      if (headerValue.startsWith(bearerTokenPrefix)) {
        val jwtToken = headerValue.drop(bearerTokenPrefix.length)
        Some(jwtToken)
      } else {
        None
      }
    }
  }

  private def extractRequestId(request: EnqueueTestSessionRequest): String = {
    request.underlying.headerMap.getOrElse("request-id", "")
  }

  final case class EnqueueTestSessionRequest(
      @RouteParam @NotBlank patientId: String,
      @RouteParam @NotBlank episodeId: String,
      @JsonProperty("uid") @NotBlank patientRef: String,
      @JsonProperty("ahc_number") @NotBlank episodeRef: String,
      @NotBlank locationId: String,
      @NotBlank firstName: String,
      @NotBlank lastName: String,
      @JsonProperty("dob") @NotNull @Past birthDate: Date,
      @NotBlank @MatchesSet(message = "Gender must be either 'MALE', 'FEMALE' or 'OTHERS'",
                            acceptedValues = Array("MALE", "FEMALE", "OTHERS")
                           ) gender: String,
      @JsonProperty("email")
      @Email(regexp = ClientEmailRegex, message = "Invalid email format")
      @Size(min = EmailMin, max = EmailMax)
      maybeEmail: Option[String],
      @JsonProperty("mobile") maybeMobileNumber: Option[Long],
      underlying: Request
  )

  final case class CreateSessionRequest(
      @RouteParam @NotBlank sessionId: String,
      @NotBlank @Size(min = UserBatchCodeMin, max = UserBatchCodeMax) userBatchCode: String,
      @NotBlank countryCode: String,
      underlying: Request
  )

  final case class UpdateSessionRequest(
      @RouteParam @NotBlank sessionId: String,
      @Email(regexp = EmailRegex, flags = Array(Pattern.Flag.CASE_INSENSITIVE)) @Size(min = EmailMin,
                                                                                      max = EmailMax
                                                                                     ) email: String,
      underlying: Request
  )

  final case class VerifySessionRequest(
      @RouteParam @NotBlank sessionId: String,
      @Email(regexp = EmailRegex, flags = Array(Pattern.Flag.CASE_INSENSITIVE)) @Size(min = EmailMin,
                                                                                      max = EmailMax
                                                                                     ) email: String,
      @Size(min = OtpMin, max = OtpMax) otp: String,
      @NotBlank name: String,
      underlying: Request
  )

  final case class LoginRequest(
      // Session id used to verify user during logging in only
      @RouteParam @NotBlank loginSessionId: String,
      @Email(regexp = EmailRegex, flags = Array(Pattern.Flag.CASE_INSENSITIVE)) @Size(min = EmailMin,
                                                                                      max = EmailMax
                                                                                     ) email: String,
      underlying: Request
  )

  final case class LoginVerifyRequest(
      // Session id used to verify user during logging in only
      @RouteParam @NotBlank loginSessionId: String,
      @Email(regexp = EmailRegex, flags = Array(Pattern.Flag.CASE_INSENSITIVE)) @Size(min = EmailMin,
                                                                                      max = EmailMax
                                                                                     ) email: String,
      @Size(min = OtpMin, max = OtpMax) otp: String,
      underlying: Request
  )

  final case class SessionResultRequest(
      @RouteParam @NotBlank sessionId: String,
      @NotBlank @Size(min = UserBatchCodeMin, max = UserBatchCodeMax) userBatchCode: String,
      underlying: Request
  )

  final case class CreateSurveyRequest(
      @RouteParam @NotBlank sessionId: String,
      surveyItems: List[SurveyItem],
      underlying: Request
  )

  final case class SurveyItem(
      key: String,
      value: String
  )

  final case class VerifySessionResponse(jwt: String)

  final case class LatestTestSessionResponse(sessionId: String, isScoreReady: Boolean)

  final case class AllowNewSessionResponse(isAllowed: Boolean)

  object Implicits {

    final implicit val EnqueueTestSessionRequestToParamsTransformer: Transformer[
      EnqueueTestSessionRequest,
      EnqueueTestSessionParams
    ] =
      Transformer
        .define[
          EnqueueTestSessionRequest,
          EnqueueTestSessionParams
        ]
        .withFieldComputed(_.requestId, src => extractRequestId(src))
        .withFieldComputed(
          _.birthDate,
          _.birthDate.toInstant.atZone(ZoneId.of("UTC")).toLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
        .buildTransformer

  }

}

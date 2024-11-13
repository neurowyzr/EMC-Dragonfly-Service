package com.neurowyzr.nw.dragon.service.api

import javax.inject.Inject

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.annotations.RouteParam
import com.twitter.util.Future
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.api.UsersController.{
  CreateConsentRequest, LatestReportRequest, UpdateUserRequest, UserWithDataConsentResponse
}
import com.neurowyzr.nw.dragon.service.api.filters.JwtFilterService
import com.neurowyzr.nw.dragon.service.biz.UserService
import com.neurowyzr.nw.dragon.service.biz.exceptions.{
  ReportNotFoundException, UserConsentNotFoundException, UserNotFoundException
}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.UpdateUserParams
import com.neurowyzr.nw.dragon.service.biz.models.UserConsentModels.CreateUserDataConsentParams
import com.neurowyzr.nw.dragon.service.di.validators.MatchesSet
import com.neurowyzr.nw.dragon.service.utils.context.JwtUtil

import com.auth0.jwt.interfaces.Claim
import io.scalaland.chimney.dsl.TransformationOps
import jakarta.validation.constraints.*

class UsersController @Inject() (service: UserService) extends Controller with Logging {

  filter[JwtFilterService] {
    get("/v1/users") { (request: Request) =>
      request.authorization.map { token =>
        val claims: Map[String, Claim] = JwtUtil.extractClaims(token.drop("Bearer ".length))
        service
          .getUserByUsername(claims("email").asString)
          .map(res => response.ok(res.into[UserWithDataConsentResponse].transform))
          .rescue { case e: UserNotFoundException => Future.value(response.notFound(ErrorMsg(e.getMessage))) }
          .rescue { case e: UserConsentNotFoundException => Future.value(response.notFound(ErrorMsg(e.getMessage))) }
      }.get
    }

    delete("/v1/users") { (request: Request) =>
      request.authorization.map { token =>
        val claims: Map[String, Claim] = JwtUtil.extractClaims(token.drop("Bearer ".length))
        service.deleteUserByUsername(claims("email").asString).map(_ => response.noContent)
      }.get
    }

    delete("/v1/users/consent") { (request: Request) =>
      request.authorization.map { token =>
        val claims: Map[String, Claim] = JwtUtil.extractClaims(token.drop("Bearer ".length))
        service.deleteUserConsentByUsername(claims("email").asString).map(_ => response.noContent).rescue {
          case e: UserNotFoundException => Future.value(response.notFound(ErrorMsg(e.getMessage)))
        }
      }.get
    }

    post("/v1/users/consent") { (request: CreateConsentRequest) =>
      request.underlying.authorization.map { token =>
        val claims: Map[String, Claim] = JwtUtil.extractClaims(token.drop("Bearer ".length))
        service
          .createUserConsent(CreateUserDataConsentParams(claims("email").asString(), request.isDataConsent))
          .map(_ => response.created)
          .rescue { case e: UserNotFoundException => Future.value(response.notFound(ErrorMsg(e.getMessage))) }
      }.get
    }

    put("/v1/users/:user_id") { (request: UpdateUserRequest) =>
      val params = request.into[UpdateUserParams].transform
      service.updateUser(params).map(_ => response.noContent).rescue { case e: UserNotFoundException =>
        Future.value(response.notFound(ErrorMsg(e.getMessage)))
      }
    }

    post("/v1/users/reports/latest") { (request: LatestReportRequest) =>
      request.underlying.authorization.map { token =>
        val claims: Map[String, Claim] = JwtUtil.extractClaims(token.drop("Bearer ".length))
        val email                      = claims("email").asString
        val userBatchCode              = request.userBatchCode
        service
          .sendLatestReport(userBatchCode, email)
          .map(_ => response.ok(s"Report is send to username: $email"))
          .rescue { case e: ReportNotFoundException => Future.value(response.notFound(ErrorMsg(e.getMessage))) }
      }.get
    }
  }

}

private object UsersController {
  // $COVERAGE-OFF$
  private final val EmailRegex   = "^[a-zA-Z0-9_!#$%&'*/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"
  private final val EmailMin     = 5
  private final val EmailMax     = 40
  private final val BirthYearMin = 1900
  private final val BirthYearMax = 2100
  // $COVERAGE-ON$

  final case class UpdateUserRequest(
      @RouteParam @NotBlank userId: String,
      @Email(regexp = EmailRegex) @Size(min = EmailMin, max = EmailMax) email: String,
      @NotBlank name: String,
      @Min(BirthYearMin) @Max(BirthYearMax) birthYear: Int,
      @NotBlank @MatchesSet(message = "Gender must be either 'MALE', 'FEMALE' or 'OTHERS'",
                            acceptedValues = Array("MALE", "FEMALE", "OTHERS")
                           ) gender: String,
      underlying: Request
  )

  final case class CreateConsentRequest(
      isDataConsent: Boolean,
      underlying: Request
  )

  final case class LatestReportRequest(userBatchCode: String, underlying: Request)

  final case class UserWithDataConsentResponse(
      email: String,
      name: String,
      birthYear: Int,
      gender: String,
      isDataConsent: Boolean
  )

}

package com.neurowyzr.nw.dragon.service.api.filters

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.http.Fields.Authorization
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.SessionService
import com.neurowyzr.nw.dragon.service.utils.context.JwtUtil

@Singleton
final class JwtFilterService @Inject() (responseBuilder: ResponseBuilder, sessionService: SessionService)
    extends SimpleFilter[Request, Response] {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    request.headerMap.get(Authorization) match {
      case Some(header) if header.startsWith("Bearer ") =>
        val jwtToken = header.drop("Bearer ".length)
        validateToken(jwtToken, service, request)
      case _ => unauthorizedResponse("Authorization header is missing.")
    }
  }

  private def validateToken(jwtToken: String, service: Service[Request, Response], request: Request): Future[Response] = {
    if (JwtUtil.isTokenValid(jwtToken)) {
      isSessionValid(jwtToken).flatMap {
        case true  => service(request)
        case false => unauthorizedResponse("Token is invalid.")
      }
    } else {
      unauthorizedResponse("Token is invalid.")
    }
  }

  private def isSessionValid(jwtToken: String): Future[Boolean] = {

    val sessionId = JwtUtil.extractClaims(jwtToken)("session_id").asString()
    val email     = JwtUtil.extractClaims(jwtToken)("email").asString()

    sessionService.getSessionOtp(sessionId, email).map {
      _.flatMap(_.maybeUtcInvalidatedAt).forall(_.isAfter(LocalDateTime.now(ZoneOffset.UTC)))
    }
  }

  private def unauthorizedResponse(message: String): Future[Response] = {
    responseBuilder.unauthorized(message).toFuture
  }

}

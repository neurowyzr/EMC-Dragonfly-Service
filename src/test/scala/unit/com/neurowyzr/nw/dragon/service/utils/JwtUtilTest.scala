package com.neurowyzr.nw.dragon.service.utils

import java.time.Instant
import java.time.temporal.ChronoUnit

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeExpiredJwt, FakeTemperedJwt, FakeValidJwt}
import com.neurowyzr.nw.dragon.service.utils.context.JwtUtil

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.Claim
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class JwtUtilTest extends AnyWordSpec with Matchers with MockitoSugar {

  "JwtUtil" should {

    "generate a valid token" in {
      val sessionId = "session123"
      val email     = "test@example.com"
      val name      = "name"
      val instant   = Instant.now.truncatedTo(ChronoUnit.SECONDS)

      val fixedClock = java.time.Clock.fixed(instant, java.time.ZoneId.systemDefault())

      val token      = JwtUtil.generateToken(sessionId, email, name)(fixedClock)
      val _          = token should not be empty
      val decodedJWT = JWT.decode(token)
      val _          = decodedJWT.getSubject shouldBe email
      val _          = decodedJWT.getId shouldBe sessionId
      val _          = decodedJWT.getIssuedAt.toInstant shouldBe instant
      val _          = decodedJWT.getNotBefore.toInstant shouldBe instant
      val _          = decodedJWT.getExpiresAt.toInstant shouldBe instant.plusSeconds(30 * 60)
    }

    "verify a valid token" in {
      JwtUtil.isTokenValid(FakeValidJwt) shouldBe true
    }

    "verify an invalid token that is tempered" in {
      JwtUtil.isTokenValid(FakeTemperedJwt) shouldBe false
    }

    "verify an invalid token that is expired" in {
      JwtUtil.isTokenValid(FakeExpiredJwt) shouldBe false
    }

    "extract claims from a valid token" in {
      val claims: Map[String, Claim] = JwtUtil.extractClaims(FakeValidJwt)

      val emailClaim   = claims.get("email").map(_.asString())
      val _            = emailClaim shouldBe Some("test@example.com")
      val sessionClaim = claims.get("session_id").map(_.asString())
      val _            = sessionClaim shouldBe Some("session123")
    }

    "extract claims from an invalid token" in {
      val claims: Map[String, Claim] = JwtUtil.extractClaims("some fake token")

      val _ = claims shouldBe empty
    }
  }

}

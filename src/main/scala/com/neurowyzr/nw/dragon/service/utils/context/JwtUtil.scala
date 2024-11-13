package com.neurowyzr.nw.dragon.service.utils.context

import java.time.Clock

import scala.jdk.CollectionConverters.MapHasAsScala

import com.twitter.util.logging.Logging

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.*
import com.auth0.jwt.interfaces.Claim

object JwtUtil extends Logging {

  // Secret key used to sign the JWT token
  private final val SecretKey: String = "FFf7mN7Z7KUHjW4S9ihz812f9cVYoAIy5s9n"

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def generateToken(sessionId: String, email: String, name: String)(implicit clock: Clock = Clock.systemUTC()): String = {
    val algorithm  = Algorithm.HMAC256(SecretKey)
    val issuedTime = java.time.Instant.now
    val expiresAt  = java.time.Instant.now.plusSeconds(30 * 60)

    JWT
      .create()
      .withSubject(email)
      .withExpiresAt(expiresAt)
      .withNotBefore(issuedTime)
      .withIssuedAt(issuedTime)
      .withJWTId(sessionId)
      .withClaim("session_id", sessionId)
      .withClaim("email", email)
      .withClaim("name", name)
      .sign(algorithm)
  }

  def isTokenValid(token: String): Boolean = {
    try {
      val algorithm = Algorithm.HMAC256(SecretKey)
      val verifier  = JWT.require(algorithm).build()
      verifier.verify(token)
      true
    } catch {
      case ex: JWTVerificationException =>
        error(s"Token is invalid. Error verifying JWT: ${ex.getMessage}")
        false
    }
  }

  def extractClaims(token: String): Map[String, Claim] = {
    try {
      val decodedJwt = JWT.decode(token)
      decodedJwt.getClaims.asScala.toMap
    } catch {
      case ex: JWTVerificationException =>
        error(s"Token is invalid. ${ex.getMessage}")
        Map.empty[String, Claim]
    }
  }

}

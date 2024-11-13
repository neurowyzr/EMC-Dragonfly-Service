package com.neurowyzr.nw.dragon.service.biz.models

object SessionModels {

  final case class CreateSessionParams(
      sessionId: String,
      userBatchCode: String,
      email: String
  )

  final case class CreateUserSessionParams(
      sessionId: String,
      userBatchCode: String,
      countryCode: String
  )

  final case class UpdateSessionParams(
      sessionId: String,
      email: String
  )

  final case class VerifySessionParams(
      sessionId: String,
      email: String,
      otp: String,
      name: String
  )

  final case class LoginParams(loginSessionId: String, email: String)

  final case class VerifyLoginParams(loginSessionId: String, email: String, otp: String)

  final case class SendUserReport(sessionId: String, userBatchCode: String)

  final case class UpdateUserParams(
      userId: String,
      email: String,
      name: String,
      birthYear: Int,
      gender: String
  )

  final case class CurrentTestSession(engagementId: Long, userBatchId: Long, userId: Long)
  final case class CheckNewSessionAllowedParams(username: String)

  final case class LatestCompletedSessionParams(email: String)

  final case class LatestTestSession(sessionId: String, isScoreReady: Boolean)

  final case class EnqueueTestSessionParams(
      requestId: String,
      patientId: String,
      episodeId: String,
      patientRef: String,
      episodeRef: String,
      locationId: String,
      firstName: String,
      lastName: String,
      birthDate: String,
      gender: String,
      maybeEmail: Option[String],
      maybeMobileNumber: Option[Long]
  )

  final case class CreateTestSessionArgs(
      requestId: String,
      patientRef: String,
      episodeRef: String,
      locationId: String
  )

}

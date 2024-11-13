package com.neurowyzr.nw.dragon.service.biz.models

import java.time.{LocalDateTime, ZoneOffset}

import com.neurowyzr.nw.dragon.service.biz.models.Defaults.{DefaultIntId, DefaultLongId}

final case class SessionOtp(id: Long,
                            sessionId: String,
                            emailHash: String,
                            otpValue: String,
                            attemptCount: Int,
                            utcCreatedAt: LocalDateTime,
                            utcExpiredAt: LocalDateTime,
                            maybeUtcInvalidatedAt: Option[LocalDateTime]
                           )

object SessionOtp {

  def apply(sessionId: String, emailHash: String, otpValue: String, utcExpiredAt: LocalDateTime): SessionOtp =
    SessionOtp(
      DefaultLongId,
      sessionId,
      emailHash,
      otpValue,
      0,
      LocalDateTime.now(ZoneOffset.UTC),
      utcExpiredAt,
      None
    )

}

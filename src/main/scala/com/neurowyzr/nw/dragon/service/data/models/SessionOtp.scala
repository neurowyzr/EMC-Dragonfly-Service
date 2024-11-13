package com.neurowyzr.nw.dragon.service.data.models

import java.time.LocalDateTime

private[data] final case class SessionOtp(id: Long,
                                          sessionId: String,
                                          emailHash: String,
                                          otpValue: String,
                                          attemptCount: Int,
                                          utcCreatedAt: LocalDateTime,
                                          utcExpiredAt: LocalDateTime,
                                          maybeUtcInvalidatedAt: Option[LocalDateTime]
                                         )

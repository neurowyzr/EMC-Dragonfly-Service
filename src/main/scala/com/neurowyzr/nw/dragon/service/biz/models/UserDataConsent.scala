package com.neurowyzr.nw.dragon.service.biz.models

import java.time.LocalDateTime

final case class UserDataConsent(userId: Long,
                                 isConsent: Boolean,
                                 utcCreatedAt: LocalDateTime,
                                 maybeUtcRevokedAt: Option[LocalDateTime]
                                )

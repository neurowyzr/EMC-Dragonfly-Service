package com.neurowyzr.nw.dragon.service.data.models

import java.time.LocalDateTime

private[data] final case class UserDataConsent(userId: Long,
                                               isConsent: Boolean,
                                               utcCreatedAt: LocalDateTime,
                                               maybeUtcRevokedAt: Option[LocalDateTime]
                                              )

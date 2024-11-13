package com.neurowyzr.nw.dragon.service.data.models

import java.time.LocalDateTime

private[data] final case class Episode(id: Long,
                                       userId: Long,
                                       episodeRef: String,
                                       testSessionId: Long,
                                       isInvalidated: Boolean,
                                       maybeMessageId: Option[String],
                                       maybeSource: Option[String],
                                       maybeUtcStartAt: Option[LocalDateTime],
                                       maybeUtcExpiryAt: Option[LocalDateTime],
                                       maybeUtcCreatedAt: Option[LocalDateTime],
                                       maybeUtcUpdatedAt: Option[LocalDateTime]
                                      )

package com.neurowyzr.nw.dragon.service.biz.models

import java.time.LocalDateTime

final case class Episode(id: Long,
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
    extends StringRepresentation

object Episode {

  def apply(userId: Long,
            episodeRef: String,
            messageId: String,
            source: String,
            utcStartAt: LocalDateTime,
            utcExpiryAt: LocalDateTime
           ): Episode = Episode(
    id = Defaults.DefaultLongId,
    userId = userId,
    episodeRef = episodeRef,
    testSessionId = Defaults.DefaultLongId,
    isInvalidated = false,
    maybeMessageId = Some(messageId),
    maybeSource = Some(source),
    maybeUtcStartAt = Some(utcStartAt),
    maybeUtcExpiryAt = Some(utcExpiryAt),
    None,
    None
  )

}

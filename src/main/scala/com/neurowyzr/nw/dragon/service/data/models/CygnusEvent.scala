package com.neurowyzr.nw.dragon.service.data.models

import java.time.LocalDateTime

private[data] final case class CygnusEvent(id: Long,
                                           messageType: String,
                                           messageId: String,
                                           maybeUtcCreatedAt: Option[LocalDateTime]
                                          )

object CygnusEvent {

  def apply(messageType: String, messageId: String): CygnusEvent = CygnusEvent(Defaults.DefaultLongId,
                                                                               messageType,
                                                                               messageId,
                                                                               None
                                                                              )

}

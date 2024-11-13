package com.neurowyzr.nw.dragon.service.data.models

import java.time.LocalDateTime

private[data] final case class Sample(id: Long, name: String, utcCreatedAt: LocalDateTime)

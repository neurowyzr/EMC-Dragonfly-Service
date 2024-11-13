package com.neurowyzr.nw.dragon.service.data.models

import java.time.{LocalDateTime, ZoneOffset}

import com.neurowyzr.nw.dragon.service.data.models.Types.{IntId, LongId}

private[data] object Defaults {

  final val DefaultLongId: LongId = -1L

  final val DefaultIntId: IntId = -1

  final val DefaultUserStatus: String = "ACTIVE"

}

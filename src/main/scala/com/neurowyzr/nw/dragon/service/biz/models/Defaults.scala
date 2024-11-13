package com.neurowyzr.nw.dragon.service.biz.models

import java.time.{LocalDateTime, ZoneOffset}

import com.neurowyzr.nw.dragon.service.biz.models.Types.{IntId, LongId}

private[biz] object Defaults {

  final val DefaultLongId: LongId = -1L

  final val DefaultIntId: IntId = -1

  final val DefaultUserStatus: String = "ACTIVE"

  final val DefaultUserRole: Long = 2L

  final val LocalDateTimeNow: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
}

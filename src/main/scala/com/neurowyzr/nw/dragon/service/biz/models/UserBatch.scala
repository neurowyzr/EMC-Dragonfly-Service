package com.neurowyzr.nw.dragon.service.biz.models

import java.time.LocalDate

final case class UserBatch(id: Long,
                           name: String,
                           status: String,
                           engagementId: Long,
                           utcEndDate: LocalDate,
                           utcStartDate: LocalDate,
                           maybeDescription: Option[String],
                           maybeCode: Option[String]
                          )

object UserBatch {

  def apply(name: String, utcStartDate: LocalDate, utcEndDate: LocalDate, engagementId: Long): UserBatch = UserBatch(
    Defaults.DefaultLongId,
    name,
    "Active",
    engagementId,
    utcEndDate,
    utcStartDate,
    None,
    None
  )

}

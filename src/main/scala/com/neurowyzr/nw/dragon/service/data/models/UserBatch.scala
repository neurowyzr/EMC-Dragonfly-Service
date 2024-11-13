package com.neurowyzr.nw.dragon.service.data.models

import java.time.LocalDate

private[data] final case class UserBatch(id: Long,
                                         name: String,
                                         status: String,
                                         engagementId: Long,
                                         utcEndDate: LocalDate,
                                         utcStartDate: LocalDate,
                                         maybeDescription: Option[String],
                                         maybeCode: Option[String]
                                        )

object UserBatch {

  def apply(name: String, startDate: LocalDate, endDate: LocalDate, engagementId: Long): UserBatch = UserBatch(
    Defaults.DefaultLongId,
    name,
    "Active",
    engagementId,
    endDate,
    startDate,
    None,
    None
  )

}

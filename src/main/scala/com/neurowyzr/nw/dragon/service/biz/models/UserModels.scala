package com.neurowyzr.nw.dragon.service.biz.models

object UserModels {

  final case class LatestReportParams(
      userBatchCode: String,
      sessionId: String
  )

}

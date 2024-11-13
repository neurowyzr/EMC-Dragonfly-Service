package com.neurowyzr.nw.dragon.service.biz.models

import java.time.LocalDate

object MagicLinkModels {

  final case class AssessmentInfo(testId: String)

  final case class CreateNewAssessmentArgs(patientRef: Int,
                                           userBatchCode: String,
                                           testId: String,
                                           expiryDate: LocalDate,
                                           source: String
                                          )

}

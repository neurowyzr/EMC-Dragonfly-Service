package com.neurowyzr.nw.dragon.service.biz.models

import java.time.LocalDateTime

object UserSurveyModels {
  final case class UserSurvey(sessionId: String, userId: Long, surveySelections: String, utcCreatedAt: LocalDateTime)

  final case class CreateUserSurveyParams(
      sessionId: String,
      username: String,
      surveyItems: List[SurveyItem]
  )

  final case class SurveyItem(
      key: String,
      value: String
  )

}

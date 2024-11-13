package com.neurowyzr.nw.dragon.service.data.models

import java.time.LocalDateTime

private[data] final case class UserSurvey(sessionId: String,
                                          userId: Long,
                                          surveySelections: String,
                                          utcCreatedAt: LocalDateTime
                                         )

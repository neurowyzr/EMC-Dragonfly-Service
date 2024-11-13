package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.UserSurveyModels.UserSurvey

trait UserSurveyRepository {
  def createUserSurvey(userSurvey: UserSurvey): Future[String]
}

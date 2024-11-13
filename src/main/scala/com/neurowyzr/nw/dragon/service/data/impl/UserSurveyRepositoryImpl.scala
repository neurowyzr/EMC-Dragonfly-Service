package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.data.{UserSurveyDao, UserSurveyRepository}
import com.neurowyzr.nw.dragon.service.data.models.UserSurvey
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class UserSurveyRepositoryImpl @Inject() (dao: UserSurveyDao, pool: FuturePool)
    extends UserSurveyRepository with Logging {

  override def createUserSurvey(userSurvey: root.biz.models.UserSurveyModels.UserSurvey): Future[String] = {
    val entity = userSurvey.into[UserSurvey].transform

    pool {
      dao.insertSurveySelections(entity).map(_ => userSurvey.sessionId)
    }.flatMap(tried => Future.const(tried))

  }

}

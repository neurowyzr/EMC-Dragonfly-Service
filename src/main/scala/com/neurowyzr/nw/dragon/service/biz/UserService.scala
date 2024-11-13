package com.neurowyzr.nw.dragon.service.biz

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.*
import com.neurowyzr.nw.dragon.service.biz.models.UserConsentModels.CreateUserDataConsentParams
import com.neurowyzr.nw.dragon.service.biz.models.UserWithDataConsent

trait UserService {
  def updateUser(params: UpdateUserParams): Future[Unit]
  def createUserConsent(params: CreateUserDataConsentParams): Future[Unit]
  def deleteUserByUsername(username: String): Future[Unit]
  def deleteUserConsentByUsername(username: String): Future[Unit]
  def getUserByUsername(username: String): Future[UserWithDataConsent]
  def sendLatestReport(userBatchCode: String, username: String): Future[String]
}

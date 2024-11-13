package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.UserDataConsent

trait UserDataConsentRepository {
  def createUserConsent(userDataConsent: UserDataConsent): Future[Long]
  def deleteUserConsentByUserId(userId: Long): Future[Long]
  def getUserConsentByUserId(userId: Long): Future[Option[UserDataConsent]]
}

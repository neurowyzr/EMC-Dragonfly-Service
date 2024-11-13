package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.biz.models.UserDataConsent
import com.neurowyzr.nw.dragon.service.data.{UserDataConsentDao, UserDataConsentRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class UserDataConsentRepositoryImpl @Inject() (dao: UserDataConsentDao, pool: FuturePool)
    extends UserDataConsentRepository with Logging {

  override def createUserConsent(userDataConsent: UserDataConsent): Future[Long] = {
    val entity = userDataConsent.into[root.data.models.UserDataConsent].transform

    pool {
      dao.insertConsent(entity).map(_ => userDataConsent.userId)
    }.flatMap(tried => Future.const(tried))
  }

  override def deleteUserConsentByUserId(userId: Long): Future[Long] = {
    pool {
      dao.revokeConsentByUserId(userId).map(_ => userId)
    }.flatMap(tried => Future.const(tried))
  }

  override def getUserConsentByUserId(userId: Long): Future[Option[UserDataConsent]] = {
    pool {
      dao.getUserConsentByUserId(userId).map(_.map(_.into[UserDataConsent].transform))
    }.flatMap(tried => Future.const(tried))
  }

}

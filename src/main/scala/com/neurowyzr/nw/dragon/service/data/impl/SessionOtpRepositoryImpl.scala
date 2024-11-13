package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.data.{SessionOtpDao, SessionOtpRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class SessionOtpRepositoryImpl @Inject() (dao: SessionOtpDao, pool: FuturePool)
    extends SessionOtpRepository with Logging {

  override def getSessionOtp(sessionId: String, emailHash: String): Future[Option[root.biz.models.SessionOtp]] = {
    pool {
      dao.getSessionOtp(sessionId, emailHash).map(maybe => maybe.map(SessionOtpRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

  override def updateSessionOtp(sessionOtp: root.biz.models.SessionOtp): Future[Long] = {
    val entity = SessionOtpRepositoryImpl.toEntity(sessionOtp)

    pool {
      dao.updateSessionOtp(entity).map(newId => newId)
    }.flatMap(tried => Future.const(tried))
  }

  override def insertSessionOtp(sessionOtp: root.biz.models.SessionOtp): Future[root.biz.models.SessionOtp] = {
    val entity = SessionOtpRepositoryImpl.toEntity(sessionOtp)

    pool {
      dao.insertSessionOtp(entity).map(newId => SessionOtpRepositoryImpl.toBiz(entity.copy(id = newId)))
    }.flatMap(tried => Future.const(tried))
  }

  override def invalidateSessionOtp(sessionId: String, emailHash: String): Future[Unit] = {
    pool {
      dao.invalidateSessionOtp(sessionId, emailHash)
    }
  }

}

private object SessionOtpRepositoryImpl {

  def toEntity(biz: root.biz.models.SessionOtp): root.data.models.SessionOtp = {
    biz.into[root.data.models.SessionOtp].transform
  }

  def toBiz(entity: root.data.models.SessionOtp): root.biz.models.SessionOtp = {
    entity.into[root.biz.models.SessionOtp].transform
  }

}

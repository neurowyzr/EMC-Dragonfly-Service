package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz
import com.neurowyzr.nw.dragon.service as root

trait SessionOtpRepository {
  def getSessionOtp(sessionId: String, emailHash: String): Future[Option[root.biz.models.SessionOtp]]

  def updateSessionOtp(userOtp: biz.models.SessionOtp): Future[Long]

  def insertSessionOtp(sessionOtp: biz.models.SessionOtp): Future[root.biz.models.SessionOtp]

  def invalidateSessionOtp(sessionId: String, emailHash: String): Future[Unit]
}

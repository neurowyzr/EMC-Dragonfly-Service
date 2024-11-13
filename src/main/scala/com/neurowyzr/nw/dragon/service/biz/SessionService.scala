package com.neurowyzr.nw.dragon.service.biz

import com.twitter.finagle.http.Response
import com.twitter.util.{Future, Try}

import com.neurowyzr.nw.dragon.service.biz.models.{SessionOtp, TaskContext}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.*
import com.neurowyzr.nw.dragon.service.biz.models.UserSurveyModels.CreateUserSurveyParams

trait SessionService {
  def enqueueTestSession(params: EnqueueTestSessionParams): Future[String]
  def createSession(params: CreateSessionParams): Future[String]
  def createUserAndSession(params: CreateUserSessionParams): Future[String]
  def updateSession(params: UpdateSessionParams): Future[Unit]
  def verifySession(params: VerifySessionParams): Future[String]
  def login(params: LoginParams): Future[Unit]
  def verifyLogin(params: VerifyLoginParams): Future[String]
  def sendUserReport(params: SendUserReport): Future[Response]
  def createUserSurvey(params: CreateUserSurveyParams): Future[Unit]
  def isNewSessionAllowedByUserName(params: CheckNewSessionAllowedParams): Future[Boolean]
  def getLatestCompletedSession(params: LatestCompletedSessionParams): Future[LatestTestSession]
  def getSessionOtp(sessionId: String, email: String): Future[Option[SessionOtp]]
  def invalidateSessionOtp(sessionId: String, email: String): Future[Unit]
}

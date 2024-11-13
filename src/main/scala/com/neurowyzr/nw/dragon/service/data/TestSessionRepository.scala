package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.{TestSession, TestSessionWithSessionId}

trait TestSessionRepository {

  def insert(testSession: TestSession): Future[TestSession]

  def getTestSessionsByUsernameAndUserBatch(username: String,
                                            userBatchCode: String
                                           ): Future[Seq[TestSessionWithSessionId]]

}

package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.data.{TestSessionDao, TestSessionRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class TestSessionRepositoryImpl @Inject() (dao: TestSessionDao, pool: FuturePool)
    extends TestSessionRepository with Logging {

  override def insert(newTestSession: root.biz.models.TestSession): Future[root.biz.models.TestSession] = {
    val entity = TestSessionRepositoryImpl.toEntity(newTestSession)

    pool {
      dao.insertNewTestSession(entity).map(newId => TestSessionRepositoryImpl.toBiz(entity.copy(id = newId)))
    }.flatMap(tried => Future.const(tried))
  }

  override def getTestSessionsByUsernameAndUserBatch(username: String,
                                                     userBatchCode: String
                                                    ): Future[Seq[root.biz.models.TestSessionWithSessionId]] = {
    pool {
      dao.getTestSessionsByUsernameAndUserBatch(username, userBatchCode).map { testSessions =>
        testSessions.map(pair => TestSessionRepositoryImpl.toBizWithSessionId(pair._1, pair._2))
      }
    }.flatMap(tried => Future.const(tried))
  }

}

private object TestSessionRepositoryImpl {

  def toEntity(biz: root.biz.models.TestSession): root.data.models.TestSession = {
    biz.into[root.data.models.TestSession].transform
  }

  def toBiz(entity: root.data.models.TestSession): root.biz.models.TestSession = {
    entity.into[root.biz.models.TestSession].transform
  }

  def toBizWithSessionId(entity: root.data.models.TestSession,
                         sessionId: String
                        ): root.biz.models.TestSessionWithSessionId = {
    entity.into[root.biz.models.TestSessionWithSessionId].withFieldConst(_.sessionId, sessionId).transform
  }

}

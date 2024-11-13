package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.data.{UserBatchDao, UserBatchRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class UserBatchRepositoryImpl @Inject() (dao: UserBatchDao, pool: FuturePool) extends UserBatchRepository with Logging {

  override def getUserBatchByCode(userBatchCode: String): Future[Option[root.biz.models.UserBatch]] = {
    pool {
      dao.getUserBatchByCode(userBatchCode).map(maybe => maybe.map(UserBatchRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

}

private object UserBatchRepositoryImpl {

  def toEntity(biz: root.biz.models.UserBatch): root.data.models.UserBatch = {
    biz.into[root.data.models.UserBatch].transform
  }

  def toBiz(entity: root.data.models.UserBatch): root.biz.models.UserBatch = {
    entity.into[root.biz.models.UserBatch].transform
  }

}

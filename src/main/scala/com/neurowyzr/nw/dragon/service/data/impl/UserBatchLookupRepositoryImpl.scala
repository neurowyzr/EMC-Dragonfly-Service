package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.biz.models.UserBatchLookup
import com.neurowyzr.nw.dragon.service.data.{UserBatchLookupDao, UserBatchLookupRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class UserBatchLookupRepositoryImpl @Inject() (dao: UserBatchLookupDao, pool: FuturePool)
    extends UserBatchLookupRepository with Logging {

  override def getUserBatchLookupByKey(key: String): Future[Option[root.biz.models.UserBatchLookup]] = {
    pool {
      dao.getUserBatchLookupByKey(key).map(maybe => maybe.map(UserBatchLookupRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

  override def getUserBatchLookupByCode(userBatchCode: String): Future[Option[UserBatchLookup]] = {
    pool {
      dao.getUserBatchLookupByCode(userBatchCode).map(maybe => maybe.map(UserBatchLookupRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

}

private object UserBatchLookupRepositoryImpl {

  def toEntity(biz: root.biz.models.UserBatchLookup): root.data.models.UserBatchLookup = {
    biz.into[root.data.models.UserBatchLookup].transform
  }

  def toBiz(entity: root.data.models.UserBatchLookup): root.biz.models.UserBatchLookup = {
    entity.into[root.biz.models.UserBatchLookup].transform
  }

}

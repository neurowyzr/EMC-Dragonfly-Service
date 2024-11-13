package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.data.{EngagementDao, EngagementRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class EngagementRepositoryImpl @Inject() (dao: EngagementDao, pool: FuturePool)
    extends EngagementRepository with Logging {

  override def getEngagementByUserBatchCode(userBatchCode: String): Future[Option[root.biz.models.Engagement]] = {
    pool {
      dao.getEngagementByUserBatchCode(userBatchCode).map(maybe => maybe.map(EngagementRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

}

private object EngagementRepositoryImpl {

  def toEntity(biz: root.biz.models.Engagement): root.data.models.Engagement = {
    biz.into[root.data.models.Engagement].transform
  }

  def toBiz(entity: root.data.models.Engagement): root.biz.models.Engagement = {
    entity.into[root.biz.models.Engagement].transform
  }

}

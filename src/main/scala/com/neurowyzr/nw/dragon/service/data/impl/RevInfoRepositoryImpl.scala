package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.data.{RevInfoDao, RevInfoRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class RevInfoRepositoryImpl @Inject() (dao: RevInfoDao, pool: FuturePool) extends RevInfoRepository with Logging {

  override def createRevInfo(revInfo: root.biz.models.RevInfo): Future[root.biz.models.RevInfo] = {
    val entity = RevInfoRepositoryImpl.toEntity(revInfo)

    pool {
      dao.insertNewRevInfo(entity).map(newId => RevInfoRepositoryImpl.toBiz(entity.copy(id = newId)))
    }.flatMap(tried => Future.const(tried))
  }

  override def getRevInfoById(id: Long): Future[Seq[root.biz.models.RevInfo]] = {
    pool {
      dao.getRevInfoById(id).map(RevInfo => RevInfo.map(RevInfoRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

}

private object RevInfoRepositoryImpl {

  def toEntity(biz: root.biz.models.RevInfo): root.data.models.RevInfo = {
    biz.into[root.data.models.RevInfo].transform
  }

  def toBiz(entity: root.data.models.RevInfo): root.biz.models.RevInfo = {
    entity.into[root.biz.models.RevInfo].transform
  }

}

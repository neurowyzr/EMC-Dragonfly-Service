package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.RevInfo

trait RevInfoRepository {
  def createRevInfo(revInfo: RevInfo): Future[RevInfo]
  def getRevInfoById(id: Long): Future[Seq[RevInfo]]
}

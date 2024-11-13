package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.Engagement

trait EngagementRepository {

  def getEngagementByUserBatchCode(userBatchCode: String): Future[Option[Engagement]]

}

package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.UserBatch

trait UserBatchRepository {
  def getUserBatchByCode(userBatchCode: String): Future[Option[UserBatch]]

}

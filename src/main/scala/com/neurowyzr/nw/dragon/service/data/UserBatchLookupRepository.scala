package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.UserBatchLookup

trait UserBatchLookupRepository {
  def getUserBatchLookupByKey(key: String): Future[Option[UserBatchLookup]]

  def getUserBatchLookupByCode(userBatchCode: String): Future[Option[UserBatchLookup]]

}

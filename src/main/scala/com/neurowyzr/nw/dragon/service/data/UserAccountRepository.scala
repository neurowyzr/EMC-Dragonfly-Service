package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.UserAccount

trait UserAccountRepository {
  def createUserAccount(userAccount: UserAccount): Future[UserAccount]
  def getUserAccountByUserIdAndUserBatchId(userId: Long, userBatchId: Long): Future[Option[UserAccount]]
}

package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.biz.models.UserAccount
import com.neurowyzr.nw.dragon.service.data.{UserAccountDao, UserAccountRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class UserAccountRepositoryImpl @Inject() (dao: UserAccountDao, pool: FuturePool)
    extends UserAccountRepository with Logging {

  override def createUserAccount(user: root.biz.models.UserAccount): Future[root.biz.models.UserAccount] = {
    val entity = UserAccountRepositoryImpl.toEntity(user)

    pool {
      dao.insertNewUserAccount(entity).map(newId => UserAccountRepositoryImpl.toBiz(entity.copy(id = newId)))
    }.flatMap(tried => Future.const(tried))
  }

  override def getUserAccountByUserIdAndUserBatchId(userId: Long, userBatchId: Long): Future[Option[UserAccount]] = {
    pool {
      dao
        .getUserAccountByUserIdAndUserBatchId(userId, userBatchId)
        .map(maybe => maybe.map(UserAccountRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

}

private object UserAccountRepositoryImpl {

  def toEntity(biz: root.biz.models.UserAccount): root.data.models.UserAccount = {
    biz.into[root.data.models.UserAccount].transform
  }

  def toBiz(entity: root.data.models.UserAccount): root.biz.models.UserAccount = {
    entity.into[root.biz.models.UserAccount].transform
  }

}

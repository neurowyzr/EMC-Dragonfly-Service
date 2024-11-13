package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.data.{UserAccountAudDao, UserAccountAudRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class UserAccountAudRepositoryImpl @Inject() (dao: UserAccountAudDao, pool: FuturePool)
    extends UserAccountAudRepository with Logging {

  override def createUserAccountAud(
      userAccountAud: root.biz.models.UserAccountAud
  ): Future[root.biz.models.UserAccountAud] = {
    val entity = UserAccountAudRepositoryImpl.toEntity(userAccountAud)

    pool {
      dao.insertNewUserAccountAud(entity).map(newId => UserAccountAudRepositoryImpl.toBiz(entity.copy(id = newId)))
    }.flatMap(tried => Future.const(tried))
  }

  override def getUserAccountAudById(id: Long): Future[Seq[root.biz.models.UserAccountAud]] = {
    pool {
      dao.getUserAccountAudById(id).map(userAccountAud => userAccountAud.map(UserAccountAudRepositoryImpl.toBiz))
    }.flatMap(tried => Future.const(tried))
  }

}

private object UserAccountAudRepositoryImpl {

  def toEntity(biz: root.biz.models.UserAccountAud): root.data.models.UserAccountAud = {
    biz.into[root.data.models.UserAccountAud].transform
  }

  def toBiz(entity: root.data.models.UserAccountAud): root.biz.models.UserAccountAud = {
    entity.into[root.biz.models.UserAccountAud].transform
  }

}

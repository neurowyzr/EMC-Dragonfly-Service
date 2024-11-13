package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.{Future, FuturePool}
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.data.{UserRoleDao, UserRoleRepository}
import com.neurowyzr.nw.dragon.service as root

import io.scalaland.chimney.dsl.*

@Singleton
class UserRoleRepositoryImpl @Inject() (dao: UserRoleDao, pool: FuturePool) extends UserRoleRepository with Logging {

  override def createUserRole(userRole: root.biz.models.UserRole): Future[root.biz.models.UserRole] = {
    val entity = UserRoleRepositoryImpl.toEntity(userRole)

    pool {
      dao.insertNewUserRole(entity).map(_ => userRole)
    }.flatMap(tried => Future.const(tried))
  }

}

private object UserRoleRepositoryImpl {

  def toEntity(biz: root.biz.models.UserRole): root.data.models.UserRole = {
    biz.into[root.data.models.UserRole].transform
  }

  def toBiz(entity: root.data.models.UserRole): root.biz.models.UserRole = {
    entity.into[root.biz.models.UserRole].transform
  }

}

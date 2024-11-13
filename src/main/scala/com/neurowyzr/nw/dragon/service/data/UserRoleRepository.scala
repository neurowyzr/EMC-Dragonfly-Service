package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.UserRole

trait UserRoleRepository {
  def createUserRole(userRole: UserRole): Future[UserRole]
}

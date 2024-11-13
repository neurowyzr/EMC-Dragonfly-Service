package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.UserAccountAud

trait UserAccountAudRepository {
  def createUserAccountAud(userAccountAud: UserAccountAud): Future[UserAccountAud]
  def getUserAccountAudById(id: Long): Future[Seq[UserAccountAud]]
}

package com.neurowyzr.nw.dragon.service.biz.models

final case class UserAccountAud(id: Long, rev: Int, maybeRevType: Option[Short], maybeUserAccountConfig: Option[String])

object UserAccountAud {

  def firstEntry(id: Long, rev: Int): UserAccountAud = UserAccountAud(
    id,
    rev,
    maybeRevType = Some(0),
    maybeUserAccountConfig = None
  )

  def lastEntry(id: Long, rev: Int): UserAccountAud = UserAccountAud(
    id,
    rev,
    maybeRevType = Some(1),
    maybeUserAccountConfig = Some("{ \"FREQUENCY\": \"DAILY\" }")
  )

}

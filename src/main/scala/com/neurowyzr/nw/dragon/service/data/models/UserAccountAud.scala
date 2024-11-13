package com.neurowyzr.nw.dragon.service.data.models

final case class UserAccountAud(id: Long, rev: Int, maybeRevType: Option[Short], maybeUserAccountConfig: Option[String])

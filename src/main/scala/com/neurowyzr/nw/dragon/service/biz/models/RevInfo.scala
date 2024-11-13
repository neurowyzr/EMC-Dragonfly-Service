package com.neurowyzr.nw.dragon.service.biz.models

final case class RevInfo(id: Int, maybeRevTimeStampInMillis: Option[Long])

object RevInfo {

  def apply(maybeRevTimeStampInMillis: Option[Long]): RevInfo = RevInfo(
    id = Defaults.DefaultIntId,
    maybeRevTimeStampInMillis = maybeRevTimeStampInMillis
  )

}

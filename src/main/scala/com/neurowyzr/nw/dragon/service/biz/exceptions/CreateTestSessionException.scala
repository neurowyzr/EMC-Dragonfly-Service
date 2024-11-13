package com.neurowyzr.nw.dragon.service.biz.exceptions

import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus

final case class CreateTestSessionException(status: CreateTestSessionStatus, errorMsg: String)
    extends Exception(errorMsg)

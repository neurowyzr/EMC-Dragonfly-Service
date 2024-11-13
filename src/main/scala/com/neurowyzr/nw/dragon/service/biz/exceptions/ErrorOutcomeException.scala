package com.neurowyzr.nw.dragon.service.biz.exceptions

import com.neurowyzr.nw.dragon.service.biz.models.Outcomes

final case class ErrorOutcomeException(outcome: Outcomes.Error, errorMsg: String) extends Exception(errorMsg)

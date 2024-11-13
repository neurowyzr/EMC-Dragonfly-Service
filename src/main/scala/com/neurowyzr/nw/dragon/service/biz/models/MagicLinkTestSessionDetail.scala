package com.neurowyzr.nw.dragon.service.biz.models

import java.util.Date

final case class MagicLinkTestSessionDetail(userId: Long,
                                            userBatchId: Long,
                                            frequency: String,
                                            testSessionOrder: Int,
                                            testSessionStart: Date,
                                            testSessionEnd: Date
                                           )

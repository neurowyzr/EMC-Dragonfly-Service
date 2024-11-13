package com.neurowyzr.nw.dragon.service.data.models

import java.time.LocalDateTime

private[data] final case class TestSession(id: Long,
                                           userId: Long,
                                           userBatchId: Long,
                                           engagementId: Long,
                                           frequency: String,
                                           testSessionOrder: Int,
                                           isVoided: Boolean,
                                           maybeUtcStartAt: Option[LocalDateTime],
                                           maybeUtcEndAt: Option[LocalDateTime],
                                           maybeUacRevision: Option[Int],
                                           maybeFindScore: Option[String],
                                           maybeAggregateScore: Option[String],
                                           maybeMcqV2: Option[String],
                                           maybeZScore: Option[String],
                                           maybeUtcCompletedAt: Option[LocalDateTime]
                                          )

object TestSession {

  def apply(userId: Long, userBatchId: Long, engagementId: Long, testSessionOrder: Int): TestSession = TestSession(
    id = Defaults.DefaultLongId,
    userId = userId,
    userBatchId = userBatchId,
    engagementId = engagementId,
    frequency = "SINGLE",
    testSessionOrder = testSessionOrder,
    isVoided = false,
    maybeUtcStartAt = None,
    maybeUtcEndAt = None,
    maybeUacRevision = None,
    maybeFindScore = None,
    maybeAggregateScore = None,
    maybeMcqV2 = None,
    maybeZScore = None,
    maybeUtcCompletedAt = None
  )

  def apply(userId: Long, userBatchId: Long, engagementId: Long): TestSession = apply(userId,
                                                                                      userBatchId,
                                                                                      engagementId,
                                                                                      1
                                                                                     )

}

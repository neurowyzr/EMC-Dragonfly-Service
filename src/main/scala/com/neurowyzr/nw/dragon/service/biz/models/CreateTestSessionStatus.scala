package com.neurowyzr.nw.dragon.service.biz.models

sealed trait CreateTestSessionStatus

object CreateTestSessionStatus {
  case object DuplicateRequestId         extends CreateTestSessionStatus
  case object DuplicateEpisodeId         extends CreateTestSessionStatus // success
  case object InvalidLocationId          extends CreateTestSessionStatus
  case object UserBatchNotFound          extends CreateTestSessionStatus // internal
  case object EngagementNotFound         extends CreateTestSessionStatus // internal
  case object UserCreationFailure        extends CreateTestSessionStatus
  case object TestSessionCreationFailure extends CreateTestSessionStatus
  case object PublishFeedbackFailure     extends CreateTestSessionStatus // internal

}

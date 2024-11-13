package com.neurowyzr.nw.dragon.service.data.impl

import com.neurowyzr.nw.dragon.service.SharedFakes.*
import com.neurowyzr.nw.dragon.service.data.models.*
import com.neurowyzr.nw.dragon.service.data.models.Defaults.{DefaultIntId, DefaultLongId}

object Fakes {

  final val FakeNewEpisode: Episode = Episode(
    FakeNewEpisodeId,
    FakeNewUserId,
    FakeNewEpisodeRef,
    FakeTestSessionId,
    FakeIsInvalidated,
    Some(FakeMessageId),
    Some(FakeSource),
    Some(FakeLocalDateTimeNow),
    Some(FakeLocalDateTimeNow),
    None,
    None
  )

  final val FakeEpisodeAlpha: Episode = Episode(
    FakeEpisodeIdAlpha,
    FakeUserIdAlpha,
    FakeEpisodeRefAlpha,
    FakeTestSessionIdAlpha,
    FakeIsInvalidated,
    Some(FakeMessageIdAlpha),
    Some(FakeSource),
    Some(FakeLocalDateTimeNow),
    Some(FakeLocalDateTimeNow),
    None,
    None
  )

  final val FakeEpisodeBravo: Episode = Episode(
    FakeEpisodeIdBravo,
    FakeUserIdBravo,
    FakeEpisodeRefBravo,
    FakeTestSessionIdBravo,
    FakeIsInvalidated,
    Some(FakeMessageIdBravo),
    Some(FakeSource),
    Some(FakeLocalDateTimeNow),
    Some(FakeLocalDateTimeNow),
    None,
    None
  )

  final val FakeUserBatch: UserBatch = UserBatch(FakeUserBatchName, FakeLocalDateNow, FakeLocalDateNow, FakeUserBatchId)

  final val FakeUserBatchLookup: UserBatchLookup = UserBatchLookup(FakeLocationId, FakeUserBatchCode)

  final val FakeEngagement: Engagement = Engagement(
    FakeClientId,
    FakeBillingPax,
    FakeProductId,
    FakeTenantToken,
    FakeEngagementName,
    FakeLocalDateNow,
    FakeLocalDateNow
  )

  final val FakeEpisodeSeq: Seq[Episode] = Seq(FakeEpisodeAlpha, FakeEpisodeBravo)

  final val FakeTestSession = TestSession(FakeNewUserId, FakeUserBatchId, FakeEngagementId)

  final val FakeTestSessionsWithSessionId = Seq(
    (FakeTestSession.copy(maybeZScore = Option(FakeZScore3)), "fake1"),
    (FakeTestSession.copy(testSessionOrder = 2, maybeZScore = Option(FakeZScore1)), "fake2"),
    (FakeTestSession.copy(testSessionOrder = 3, maybeZScore = Option.empty), "fake3"),
    (FakeTestSession.copy(testSessionOrder = 4, maybeZScore = Option(FakeZScore2)), "fake4")
  )

  final val FakeClient: Client = Client(FakeClientName)

  final val FakeProduct: Product = Product(FakeProductName)

  final val FakeUser: User = User(FakeUserName, FakeUserPassword, FakeFirstName, FakeSource, FakeExternalPatientRef)

  final val FakeUserAccount: UserAccount = UserAccount(FakeNewUserId, FakeUserBatchId)

  final val FakeUserAccountAud: UserAccountAud = UserAccountAud(FakeUserAccountId,
                                                                FakeRev,
                                                                Some(FakeRevType),
                                                                Some(FakeUserAccountConfig)
                                                               )

  final val FakeRevInfo: RevInfo = RevInfo(FakeRev, Some(FakeRevTimeStampInMillis))

  final val FakeSessionOtp: SessionOtp = SessionOtp(
    DefaultLongId,
    FakeSessionId,
    FakeValidEmail,
    FakeValidOtp,
    DefaultIntId,
    FakeLocalDateTimeNowAlpha,
    FakeLocalDateTimeFuture,
    None
  )

  final val FakeUserRole: UserRole = UserRole(FakeNewUserId, FakeRoleId)

  final val FakeUserSurvey = UserSurvey(FakeSessionId, FakeUserIdAlpha, FakeSurveySelections, FakeLocalDateTimeNowAlpha)

  final val FakeUserConsent = UserDataConsent(FakeUserIdAlpha, true, FakeLocalDateTimeNowAlpha, None)
}

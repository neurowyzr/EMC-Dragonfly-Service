package com.neurowyzr.nw.dragon.service.biz.impl

import java.time.LocalDateTime
import java.util.Date

import com.neurowyzr.nw.dragon.service.SharedFakes.*
import com.neurowyzr.nw.dragon.service.biz.models.*
import com.neurowyzr.nw.dragon.service.biz.models.Defaults.{DefaultIntId, DefaultLongId}
import com.neurowyzr.nw.dragon.service.biz.models.UserSurveyModels.UserSurvey

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

  final val FakeTestSessionWithSessionId = TestSessionWithSessionId(FakeNewUserId,
                                                                    FakeUserBatchId,
                                                                    FakeEngagementId,
                                                                    FakeNewEpisodeRef
                                                                   )

  final val FakeTestSessions = Seq(
    FakeTestSessionWithSessionId.copy(testSessionOrder = 2,
                                      maybeZScore = Option(FakeZScore1),
                                      maybeUtcCompletedAt = Some(LocalDateTime.now.minusDays(1)),
                                      sessionId = "fakeRef2"
                                     ),
    FakeTestSessionWithSessionId.copy(maybeZScore = Option(FakeZScore3),
                                      maybeUtcCompletedAt = Some(LocalDateTime.now.minusDays(4)),
                                      sessionId = "fakeRef1"
                                     ),
    FakeTestSessionWithSessionId.copy(testSessionOrder = 3,
                                      maybeZScore = Option.empty,
                                      maybeUtcCompletedAt = Some(LocalDateTime.now.minusDays(3)),
                                      sessionId = "fakeRef3"
                                     ),
    FakeTestSessionWithSessionId.copy(testSessionOrder = 4,
                                      maybeZScore = Option(FakeZScore2),
                                      maybeUtcCompletedAt = Some(LocalDateTime.now.minusDays(2)),
                                      sessionId = "fakeRef4"
                                     )
  )

  final val FakeUser: User = User(FakeUserName, FakeUserPassword, FakeFirstName, FakeSource, FakeExternalPatientRef)

  final val FakeCreatedUser: User = User(
    id = FakeCreatedUserId,
    username = FakeUserName,
    password = FakeUserPassword,
    firstName = FakeFirstName,
    maybeEmailHash = None,
    maybeLastName = None,
    maybeDateOfBirth = None,
    maybeMobileNumber = None,
    maybeCountry = None,
    maybeNationality = None,
    maybePostalCode = None,
    maybeAddress = None,
    maybeGender = None,
    maybeUserStatus = Some(Defaults.DefaultUserStatus),
    maybeUserProfile = None,
    maybeIcOrPassportNumber = None,
    maybeExternalPatientRef = Some(FakeExternalPatientRef),
    maybeSource = Some(FakeSource),
    maybeUtcCreatedAt = None,
    maybeUtcUpdatedAt = None
  )

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

  final val FakeMagicLinkTestSessionDetail: MagicLinkTestSessionDetail = MagicLinkTestSessionDetail(FakeUserIdAlpha,
                                                                                                    FakeUserBatchId,
                                                                                                    "SINGLE",
                                                                                                    1,
                                                                                                    new Date(),
                                                                                                    new Date()
                                                                                                   )

  final val FakeUserDataConsent = UserDataConsent(FakeNewUserId, true, FakeLocalDateTimeNow, None)

  final val FakeUserSurvey = UserSurvey(FakeSessionId, FakeUserIdAlpha, FakeSurveySelections, FakeLocalDateTimeNowAlpha)

}

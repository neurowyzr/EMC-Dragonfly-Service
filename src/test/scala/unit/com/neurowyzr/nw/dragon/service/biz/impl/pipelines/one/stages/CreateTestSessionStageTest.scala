package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.*
import com.neurowyzr.nw.dragon.service.biz.exceptions.CreateTestSessionException
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.{
  FakeEpisodeAlpha, FakeMagicLinkTestSessionDetail, FakeRevInfo, FakeTestSession, FakeUser, FakeUserAccount,
  FakeUserAccountAud, FakeUserRole
}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages.CreateTestSessionStageTest.{
  FakeOutputNewPatient, FakeOutputRepeatedPatient
}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages.CreateUserStageTest.FakeTask
import com.neurowyzr.nw.dragon.service.biz.impl.stages.FakeCreateTestSessionTask
import com.neurowyzr.nw.dragon.service.biz.models.{
  CreateTestSessionTaskOutput, Episode, RevInfo, TestSession, User, UserAccount, UserAccountAud, UserRole
}
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus.UserCreationFailure
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.{PatientRefExist, PatientRefNotFound, Success}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.CurrentTestSession
import com.neurowyzr.nw.dragon.service.clients.CoreHttpClient
import com.neurowyzr.nw.dragon.service.data.{
  EpisodeRepository, RevInfoRepository, UserAccountAudRepository, UserAccountRepository, UserRepository,
  UserRoleRepository
}

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CreateTestSessionStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockUserRoleRepo: UserRoleRepository             = mock[UserRoleRepository]
  val mockUserAccountRepo: UserAccountRepository       = mock[UserAccountRepository]
  val mockRevInfoRepo: RevInfoRepository               = mock[RevInfoRepository]
  val mockUserAccountAudRepo: UserAccountAudRepository = mock[UserAccountAudRepository]
  val mockEpisodeRepo: EpisodeRepository               = mock[EpisodeRepository]
  val mockCoreHttpClient: CoreHttpClient               = mock[CoreHttpClient]

  private val testInstance =
    new CreateTestSessionStage(
      mockUserRoleRepo,
      mockUserAccountRepo,
      mockRevInfoRepo,
      mockUserAccountAudRepo,
      mockEpisodeRepo,
      FakeDbfsConfig,
      FakeCustomerConfig,
      mockCoreHttpClient
    )

  "test session created for first-time repeated patient" in {
    mockUserAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) returns Future.value(Some(FakeUserAccount))
    mockUserAccountAudRepo.getUserAccountAudById(*[Long]) returns Future.value(Seq(FakeUserAccountAud))
    mockRevInfoRepo.createRevInfo(*[RevInfo]) returns Future.value(FakeRevInfo)
    mockUserAccountAudRepo.createUserAccountAud(*[UserAccountAud]) returns Future.value(FakeUserAccountAud)

    mockCoreHttpClient.getCurrentTestSession(*[CurrentTestSession]) returns Future.value(FakeMagicLinkTestSessionDetail)
    mockEpisodeRepo.insertEpisodeAndTestSession(*[Episode], *[TestSession]) returns Future.value(
      (FakeEpisodeAlpha, FakeTestSession)
    )

    val fakeTask = FakeTask.copy(out = FakeOutputRepeatedPatient)
    val result   = Await.result(testInstance.execute(fakeTask))

    val validTaskMod = fakeTask
      .modify(_.out.maybeMagicLinkUrl)
      .setTo(Some(FakeDbfsConfig.magicLinkPath + "/" + FakeTask.in.episodeRef))
      .modify(_.out.maybeOutcome)
      .setTo(Some(Success))

    result shouldBe validTaskMod
    mockUserAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
    mockUserAccountAudRepo.getUserAccountAudById(*[Long]) wasCalled once
    mockRevInfoRepo.createRevInfo(*[RevInfo]) wasCalled once
    mockUserAccountAudRepo.createUserAccountAud(*[UserAccountAud]) wasCalled once

    mockCoreHttpClient.getCurrentTestSession(*[CurrentTestSession]) wasCalled once
    mockEpisodeRepo.insertEpisodeAndTestSession(*[Episode], *[TestSession]) wasCalled once
  }

  "test session created for multiple times repeated patient" in {
    mockUserAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) returns Future.value(Some(FakeUserAccount))
    mockUserAccountAudRepo.getUserAccountAudById(*[Long]) returns Future.value(
      Seq(FakeUserAccountAud, FakeUserAccountAud)
    )

    mockCoreHttpClient.getCurrentTestSession(*[CurrentTestSession]) returns Future.value(FakeMagicLinkTestSessionDetail)
    mockEpisodeRepo.insertEpisodeAndTestSession(*[Episode], *[TestSession]) returns Future.value(
      (FakeEpisodeAlpha, FakeTestSession)
    )

    val fakeTask = FakeTask.copy(out = FakeOutputRepeatedPatient)
    val result   = Await.result(testInstance.execute(fakeTask))

    val validTaskMod = fakeTask
      .modify(_.out.maybeMagicLinkUrl)
      .setTo(Some(FakeDbfsConfig.magicLinkPath + "/" + FakeTask.in.episodeRef))
      .modify(_.out.maybeOutcome)
      .setTo(Some(Success))

    result shouldBe validTaskMod
    mockUserAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
    mockUserAccountAudRepo.getUserAccountAudById(*[Long]) wasCalled once

    mockCoreHttpClient.getCurrentTestSession(*[CurrentTestSession]) wasCalled once
    mockEpisodeRepo.insertEpisodeAndTestSession(*[Episode], *[TestSession]) wasCalled once
  }

  "test session created for new patient" in {
    mockUserRoleRepo.createUserRole(*[UserRole]) returns Future.value(FakeUserRole)
    mockUserAccountRepo.createUserAccount(*[UserAccount]) returns Future.value(FakeUserAccount)
    mockRevInfoRepo.createRevInfo(*[RevInfo]) returns Future.value(FakeRevInfo)
    mockUserAccountAudRepo.createUserAccountAud(*[UserAccountAud]) returns Future.value(FakeUserAccountAud)

    mockCoreHttpClient.getCurrentTestSession(*[CurrentTestSession]) returns Future.value(FakeMagicLinkTestSessionDetail)
    mockEpisodeRepo.insertEpisodeAndTestSession(*[Episode], *[TestSession]) returns Future.value(
      (FakeEpisodeAlpha, FakeTestSession)
    )

    val fakeTask = FakeTask.copy(out = FakeOutputNewPatient)
    val result   = Await.result(testInstance.execute(fakeTask))

    val validTaskMod = fakeTask
      .modify(_.out.maybeMagicLinkUrl)
      .setTo(Some(FakeDbfsConfig.magicLinkPath + "/" + FakeTask.in.episodeRef))
      .modify(_.out.maybeOutcome)
      .setTo(Some(Success))

    result shouldBe validTaskMod
    mockUserRoleRepo.createUserRole(*[UserRole]) wasCalled once
    mockUserAccountRepo.createUserAccount(*[UserAccount]) wasCalled once
    mockRevInfoRepo.createRevInfo(*[RevInfo]) wasCalled once
    mockUserAccountAudRepo.createUserAccountAud(*[UserAccountAud]) wasCalled once

    mockCoreHttpClient.getCurrentTestSession(*[CurrentTestSession]) wasCalled once
    mockEpisodeRepo.insertEpisodeAndTestSession(*[Episode], *[TestSession]) wasCalled once
  }

  "stage aborted for empty user account aud for repeated user" in {
    mockUserAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) returns Future.value(Some(FakeUserAccount))
    mockUserAccountAudRepo.getUserAccountAudById(*[Long]) returns Future.value(Seq())

    val fakeTask = FakeTask.copy(out = FakeOutputRepeatedPatient)
    val thrown = intercept[CreateTestSessionException] {
      Await.result(testInstance.execute(fakeTask))
    }

    val expectedThrownMessage =
      "User account aud is empty or more than 2 entries for repeated user " +
        s"with userId '${FakeUserIdAlpha.toString}' and userBatchId '${FakeUserBatchId.toString}'. "

    thrown.getMessage shouldBe expectedThrownMessage
    mockUserAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
    mockUserAccountAudRepo.getUserAccountAudById(*[Long]) wasCalled once
  }

  "staged aborted for user account entry not found for repeated user" in {
    mockUserAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) returns Future.value(None)

    val fakeTask = FakeTask.copy(out = FakeOutputRepeatedPatient)
    val thrown = intercept[CreateTestSessionException] {
      Await.result(testInstance.execute(fakeTask))
    }

    val expectedThrownMessage =
      s"User account entry not found for userId '${FakeUserIdAlpha.toString}' " +
        s"and userBatchId '${FakeUserBatchId.toString}'. "

    thrown.getMessage shouldBe expectedThrownMessage
    mockUserAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
  }

  "staged aborted for exception thrown for new user" in {
    mockUserRoleRepo.createUserRole(*[UserRole]) returns Future.exception(
      CreateTestSessionException(UserCreationFailure, "test exception")
    )

    val fakeTask = FakeTask.copy(out = FakeOutputNewPatient)
    val thrown = intercept[CreateTestSessionException] {
      Await.result(testInstance.execute(fakeTask))
    }

    thrown.getMessage shouldBe "test exception"
    mockUserRoleRepo.createUserRole(*[UserRole]) wasCalled once
  }

}

object CreateTestSessionStageTest {

  final val FakeOutputPopulated = CreateTestSessionTaskOutput(
    Some(FakeUserIdAlpha),
    Some(FakeUserBatchId),
    Some(FakeUserBatchCode),
    Some(FakeEngagementId),
    None,
    None
  )

  final val FakeOutputRepeatedPatient = FakeOutputPopulated.copy(maybeOutcome = Some(PatientRefExist))

  final val FakeOutputNewPatient = FakeOutputPopulated.copy(maybeOutcome = Some(PatientRefNotFound))

}

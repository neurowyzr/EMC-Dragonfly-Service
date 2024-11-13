package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.{
  FakeCustomerConfig, FakeDbfsConfig, FakeEngagementId, FakeUserBatchCode, FakeUserBatchId
}
import com.neurowyzr.nw.dragon.service.biz.exceptions.{BizException, CreateTestSessionException}
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.{FakeCreatedUser, FakeUser}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages.CreateUserStageTest.FakeTask
import com.neurowyzr.nw.dragon.service.biz.impl.stages.FakeCreateTestSessionTask
import com.neurowyzr.nw.dragon.service.biz.models.{CreateTestSessionTaskOutput, User}
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.{PatientRefExist, PatientRefNotFound}
import com.neurowyzr.nw.dragon.service.data.UserRepository

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CreateUserStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: UserRepository = mock[UserRepository]
  private val testInstance     = new CreateUserStage(mockRepo, FakeDbfsConfig, FakeCustomerConfig)

  "outcome patient exist if patient ref exists" in {
    val _ = mockRepo.getUserBySourceAndExternalPatientRef(*[String], *[String]) returns Future.value(Some(FakeUser))

    val result = Await.result(testInstance.execute(FakeTask))

    val validTaskMod = FakeTask
      .modify(_.out.maybeUserId)
      .setTo(Some(FakeUser.id))
      .modify(_.out.maybeOutcome)
      .setTo(Some(PatientRefExist))

    val _ = result shouldBe validTaskMod
    val _ = mockRepo.getUserBySourceAndExternalPatientRef(*[String], *[String]) wasCalled once
    val _ = mockRepo wasNever calledAgain

  }

  "outcome patient not found if patient ref does not exist" in {
    val _ = mockRepo.getUserBySourceAndExternalPatientRef(*[String], *[String]) returns Future.value(None)
    val _ = mockRepo.createUser(*[User]) returns Future.value(FakeCreatedUser)

    val result = Await.result(testInstance.execute(FakeTask))

    val validTaskMod = FakeTask
      .modify(_.out.maybeUserId)
      .setTo(Some(FakeCreatedUser.id))
      .modify(_.out.maybeOutcome)
      .setTo(Some(PatientRefNotFound))

    val _ = result shouldBe validTaskMod
    val _ = mockRepo.getUserBySourceAndExternalPatientRef(*[String], *[String]) wasCalled once
    val _ = mockRepo.createUser(*[User]) wasCalled once
    val _ = mockRepo wasNever calledAgain
  }

  "abort stage if exception is thrown" in {
    val _ = mockRepo.getUserBySourceAndExternalPatientRef(*[String], *[String]) returns Future.value(None)
    val _ = mockRepo.createUser(*[User]) returns Future.exception(BizException("test exception"))

    val thrown = intercept[CreateTestSessionException] {
      Await.result(testInstance.execute(FakeTask))
    }

    val _ = thrown.getMessage shouldBe "test exception"
    val _ = mockRepo.getUserBySourceAndExternalPatientRef(*[String], *[String]) wasCalled once
    val _ = mockRepo.createUser(*[User]) wasCalled once
    val _ = mockRepo wasNever calledAgain
  }

}

object CreateUserStageTest {

  final val FakeOutputPopulated = CreateTestSessionTaskOutput(
    None,
    Some(FakeUserBatchId),
    Some(FakeUserBatchCode),
    Some(FakeEngagementId),
    None,
    None
  )

  final val FakeTask = FakeCreateTestSessionTask.copy(out = FakeOutputPopulated)
}

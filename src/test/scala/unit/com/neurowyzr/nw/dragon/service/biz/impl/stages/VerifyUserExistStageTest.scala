package com.neurowyzr.nw.dragon.service.biz.impl.stages

import java.util.Date

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.exceptions.ErrorOutcomeException
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.VerifyUserExistStage
import com.neurowyzr.nw.dragon.service.biz.impl.stages.ValidateInputStageTest.{FakeTaskContextPopulated, FakeUser}
import com.neurowyzr.nw.dragon.service.biz.models.{TaskContext, User}
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.PatientNotFound
import com.neurowyzr.nw.dragon.service.data.UserRepository

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VerifyUserExistStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockUserRepo: UserRepository       = mock[UserRepository]
  val testInstance: VerifyUserExistStage = new VerifyUserExistStage(mockUserRepo)

  "pass if found user by patient id" in {
    val _         = mockUserRepo.getUserByExternalPatientRef(*[String]) returns Future.value(Some(FakeUser))
    val validTask = FakeCreateMagicLinkTask.copy(ctx = FakeTaskContextPopulated)

    val result = Await.result(testInstance.execute(validTask))

    val validTaskMod = validTask.modify(_.out.userId).setTo(Some(FakeUser.id))
    val _            = result shouldBe validTaskMod
  }

  "outcome error if user is not found by patient id" in {
    val _         = mockUserRepo.getUserByExternalPatientRef(*[String]) returns Future.value(None)
    val validTask = FakeCreateMagicLinkTask.copy(ctx = FakeTaskContextPopulated)

    val thrown = intercept[ErrorOutcomeException] {
      Await.result(testInstance.execute(validTask))
    }

    val _ = thrown.outcome shouldBe PatientNotFound
  }

}

object ValidateInputStageTest {
  final val FakeUser: User = User("fake-username", "fake-password", "fake-first-name", "fake-source", "fake-patient-id")

  final val FakeTaskContextPopulated: TaskContext = TaskContext(
    Some(new Date()),
    Some("appId"),
    Some("type"),
    Some("messageId"),
    Some("expiration"),
    Some("correlationId")
  )

}

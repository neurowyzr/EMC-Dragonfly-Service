package com.neurowyzr.nw.dragon.service.biz.impl.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeUser
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.VerifyPatientRefExistStage
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.PatientRefExist
import com.neurowyzr.nw.dragon.service.data.UserRepository

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VerifyPatientRefExistStageTest
    extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: UserRepository = mock[UserRepository]
  private val testInstance     = new VerifyPatientRefExistStage(mockRepo)

  "outcome error if user found" in {
    val _ = mockRepo.getUserBySourceAndExternalPatientRef(*[String], *[String]) returns Future.value(Some(FakeUser))

    val result = Await.result(testInstance.execute(FakeCreateUserTask))

    val _ = result.out.maybeOutcome shouldBe Some(PatientRefExist)
    val _ = mockRepo.getUserBySourceAndExternalPatientRef(*[String], *[String]) wasCalled once
    val _ = mockRepo wasNever calledAgain

  }

  "continue if user is not found" in {
    val _ = mockRepo.getUserBySourceAndExternalPatientRef(*[String], *[String]) returns Future.value(None)

    val result = Await.result(testInstance.execute(FakeCreateUserTask))

    val _ = result shouldBe FakeCreateUserTask
    val _ = mockRepo.getUserBySourceAndExternalPatientRef(*[String], *[String]) wasCalled once
    val _ = mockRepo wasNever calledAgain
  }

}

package com.neurowyzr.nw.dragon.service.biz.impl.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.NotifyCygnusStage
import com.neurowyzr.nw.dragon.service.biz.models.{CreateUserTask, Outcome, Outcomes, TaskContext}
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.PatientNotFound
import com.neurowyzr.nw.dragon.service.mq.impl.CygnusPublisherImpl

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class NotifyCygnusStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  private val mockPublisher = mock[CygnusPublisherImpl]
  private val testInstance  = new NotifyCygnusStage[CreateUserTask](mockPublisher)

  "publish message to update outcome" should {
    "succeed and send a success msg" in {
      val task   = FakeCreateUserTask.modify(_.out.maybeOutcome).setTo(Some(Outcomes.Success))
      val captor = ArgCaptor[Outcome]
      val _      = mockPublisher.publishOutcome(*[TaskContext], captor) returns Future {}

      val result = Await.result(testInstance.execute(task))

      val _ = captor.value shouldBe Outcomes.Success
      result shouldBe task
    }

    "failed and send an error msg" in {
      val task   = FakeCreateUserTask.modify(_.out.maybeOutcome).setTo(Some(PatientNotFound))
      val captor = ArgCaptor[Outcome]
      val _      = mockPublisher.publishOutcome(*[TaskContext], captor) returns Future {}

      val result = Await.result(testInstance.execute(task))

      val _ = captor.value shouldBe PatientNotFound
      result shouldBe task
    }
  }

  "skip if outcome is others" in {
    val thrown = intercept[Exception] {
      Await.result(testInstance.execute(FakeCreateUserTask))
    }

    thrown.getMessage shouldBe s"There is no outcome for task ${FakeCreateUserTask.ctx.messageId}"
  }

}

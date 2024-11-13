package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.two.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.exceptions.{CreateTestSessionException, PublisherException}
import com.neurowyzr.nw.dragon.service.biz.impl.stages.{
  FakeCreateTestSessionTask, FakeNotifyClientTask, FakeNotifyClientTaskInput
}
import com.neurowyzr.nw.dragon.service.biz.models.{NotifyClientTaskInput, Outcomes, TaskContext}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.CreateTestSessionArgs
import com.neurowyzr.nw.dragon.service.clients.CustomerHttpClient
import com.neurowyzr.nw.dragon.service.mq.impl.SelfPublisherImpl
import com.neurowyzr.nw.finatra.lib.clients.AlerterHttpClient
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.Message

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class NotifyClientStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  private val mockCustomerClient = mock[CustomerHttpClient]
  private val testInstance       = new NotifyClientStage(mockCustomerClient)

  "notify client" should {
    "succeed for success feedback" in {
      val task      = FakeNotifyClientTask
      val urlCaptor = ArgCaptor[String]
      val _         = mockCustomerClient.notifyCreateSucceeded(*[CreateTestSessionArgs], urlCaptor) returns Future {}

      val result = Await.result(testInstance.execute(task))

      val _ = urlCaptor.value shouldBe "fake-magic-link"
      result shouldBe task
      mockCustomerClient.notifyCreateSucceeded(*[CreateTestSessionArgs], *[String]) wasCalled once
      mockCustomerClient wasNever calledAgain
    }

    "succeed for failure feedback" in {
      val input = FakeNotifyClientTaskInput.copy(maybeMagicLink = None)
      val task  = FakeNotifyClientTask.copy(in = input)
      val _     = mockCustomerClient.notifyCreateFailed(*[CreateTestSessionArgs], 0) returns Future {}

      val result = Await.result(testInstance.execute(task))

      result shouldBe task
      mockCustomerClient.notifyCreateFailed(*[CreateTestSessionArgs], 0) wasCalled once
      mockCustomerClient wasNever calledAgain
    }

  }

}

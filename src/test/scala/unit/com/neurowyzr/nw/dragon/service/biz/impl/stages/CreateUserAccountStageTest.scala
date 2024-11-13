package com.neurowyzr.nw.dragon.service.biz.impl.stages

import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeUserAccount
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.CreateUserAccountStage
import com.neurowyzr.nw.dragon.service.biz.models.UserAccount
import com.neurowyzr.nw.dragon.service.data.UserAccountRepository

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CreateUserAccountStageTest extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  private val mockRepo     = mock[UserAccountRepository]
  private val testInstance = new CreateUserAccountStage(mockRepo)

  "create user account if record not found" in {
    val _ = mockRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) returns Future.value(None)
    val fakeTaskWithMissingMessageId = FakeCreateMagicLinkTask
      .modify(_.out.userId)
      .setTo(Some(FakeUserAccount.userId))
      .modify(_.out.userBatchId)
      .setTo(Some(FakeUserAccount.userBatchId))
    val _ = mockRepo.createUserAccount(*[UserAccount]) returns Future.value(FakeUserAccount)

    val result = Await.result(testInstance.execute(fakeTaskWithMissingMessageId))

    val _ = result.out.maybeOutcome shouldBe None
    val _ = mockRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
    val _ = mockRepo.createUserAccount(*[UserAccount]) wasCalled once
    val _ = mockRepo wasNever calledAgain
  }

  "skip if user account is already created" in {
    val _ = mockRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) returns Future.value(Some(FakeUserAccount))
    val fakeTaskWithMissingMessageId = FakeCreateMagicLinkTask
      .modify(_.out.userId)
      .setTo(Some(FakeUserAccount.userId))
      .modify(_.out.userBatchId)
      .setTo(Some(FakeUserAccount.userBatchId))

    val result = Await.result(testInstance.execute(fakeTaskWithMissingMessageId))

    val _ = result.out.maybeOutcome shouldBe None
    val _ = mockRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
    val _ = mockRepo wasNever calledAgain
  }

}

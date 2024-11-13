package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.inject.TestMixin
import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeNewUserId, FakeUserBatchId, FakeUserIdBravo}
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeUserAccount
import com.neurowyzr.nw.dragon.service.data.UserAccountDao
import com.neurowyzr.nw.dragon.service.data.impl.UserRepositoryImplTest.TestException
import com.neurowyzr.nw.dragon.service as root

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class UserAccountRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao      = mock[UserAccountDao]
  private val testInstance = new UserAccountRepositoryImpl(mockDao, pool)

  "createAccount" should {
    "return a user account with a new ID" in {
      val _ = mockDao.insertNewUserAccount(*[root.data.models.UserAccount]) returns Try(FakeUserIdBravo)

      val newUserAccountId = await(testInstance.createUserAccount(root.biz.impl.Fakes.FakeUserAccount))

      val _ = newUserAccountId shouldBe FakeUserAccount.modify(_.id).setTo(FakeUserIdBravo)
      val _ = mockDao.insertNewUserAccount(*[root.data.models.UserAccount]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "throw error" in {
    val _ = mockDao.insertNewUserAccount(*[root.data.models.UserAccount]) throws TestException

    val thrown = intercept[Exception] {
      await(testInstance.createUserAccount(root.biz.impl.Fakes.FakeUserAccount))
    }

    val _ = thrown.getMessage shouldBe "Exception was thrown"
    val _ = mockDao.insertNewUserAccount(*[root.data.models.UserAccount]) wasCalled once
    val _ = mockDao wasNever calledAgain
  }

  "get user account by user id and user batch is" should {
    "succeed" in {
      val _ =
        mockDao.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) returns Try(
          Some(root.data.impl.Fakes.FakeUserAccount)
        )

      val maybeReturnedUser = await(testInstance.getUserAccountByUserIdAndUserBatchId(FakeNewUserId, FakeUserBatchId))

      val _ = maybeReturnedUser.size shouldBe 1
      val _ = maybeReturnedUser.get shouldBe FakeUserAccount

      val _ = mockDao.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail" in {
      val _ = mockDao.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) returns Try(None)

      val maybeReturnedUser = await(testInstance.getUserAccountByUserIdAndUserBatchId(FakeNewUserId, FakeUserBatchId))

      val _ = maybeReturnedUser.size shouldBe 0

      val _ = mockDao.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) throws TestException

      val thrown = intercept[Exception] {
        await(testInstance.getUserAccountByUserIdAndUserBatchId(FakeNewUserId, FakeUserBatchId))
      }

      val _ = thrown.getMessage shouldBe "Exception was thrown"
      val _ = mockDao.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

private object UserAccountRepositoryImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")
}

package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.inject.TestMixin
import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeUserBatchCode
import com.neurowyzr.nw.dragon.service.data.UserBatchDao
import com.neurowyzr.nw.dragon.service.data.impl.UserRepositoryImplTest.TestException
import com.neurowyzr.nw.dragon.service as root

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class UserBatchRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao      = mock[UserBatchDao]
  private val testInstance = new UserBatchRepositoryImpl(mockDao, pool)

  "get user batch by user batch code" should {
    "succeed" in {
      val dataUserBatch    = root.data.impl.Fakes.FakeUserBatch
      val dataUserWithCode = dataUserBatch.copy(maybeCode = Some(FakeUserBatchCode))
      val bizUserBatch     = root.biz.impl.Fakes.FakeUserBatch
      val bizUserWithCode  = bizUserBatch.copy(maybeCode = Some(FakeUserBatchCode))

      val _ = mockDao.getUserBatchByCode(*[String]) returns Try(Some(dataUserWithCode))

      val maybeReturnedUser = await(testInstance.getUserBatchByCode(FakeUserBatchCode))

      val _ = maybeReturnedUser.size shouldBe 1
      val _ = maybeReturnedUser.get shouldBe bizUserWithCode

      val _ = mockDao.getUserBatchByCode(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail" in {
      val _ = mockDao.getUserBatchByCode(*[String]) returns Try(None)

      val maybeReturnedUser = await(testInstance.getUserBatchByCode(FakeUserBatchCode))

      val _ = maybeReturnedUser.size shouldBe 0

      val _ = mockDao.getUserBatchByCode(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.getUserBatchByCode(*[String]) throws TestException

      val thrown = intercept[Exception] {
        await(testInstance.getUserBatchByCode(FakeUserBatchCode))
      }

      val _ = thrown.getMessage shouldBe "Exception was thrown"
      val _ = mockDao.getUserBatchByCode(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

private object UserBatchRepositoryImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")
}

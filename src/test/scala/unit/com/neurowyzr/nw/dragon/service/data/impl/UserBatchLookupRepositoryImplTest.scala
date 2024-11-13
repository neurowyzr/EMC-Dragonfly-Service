package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.inject.TestMixin
import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeLocationId, FakeUserBatchCode}
import com.neurowyzr.nw.dragon.service.data.UserBatchLookupDao
import com.neurowyzr.nw.dragon.service.data.impl.UserBatchLookupRepositoryImplTest.TestException
import com.neurowyzr.nw.dragon.service as root

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class UserBatchLookupRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao      = mock[UserBatchLookupDao]
  private val testInstance = new UserBatchLookupRepositoryImpl(mockDao, pool)

  val dataUserBatchLookup = root.data.impl.Fakes.FakeUserBatchLookup
  val bizUserBatchLookup  = root.biz.impl.Fakes.FakeUserBatchLookup

  "get user batch lookup by key" should {
    "succeed" in {
      val _ = mockDao.getUserBatchLookupByKey(*[String]) returns Try(Some(dataUserBatchLookup))

      val maybeReturnedUser = await(testInstance.getUserBatchLookupByKey(FakeLocationId))

      val _ = maybeReturnedUser.size shouldBe 1
      val _ = maybeReturnedUser.get shouldBe bizUserBatchLookup

      val _ = mockDao.getUserBatchLookupByKey(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail" in {
      val _ = mockDao.getUserBatchLookupByKey(*[String]) returns Try(None)

      val maybeReturnedUser = await(testInstance.getUserBatchLookupByKey(FakeLocationId))

      val _ = maybeReturnedUser.size shouldBe 0

      val _ = mockDao.getUserBatchLookupByKey(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.getUserBatchLookupByKey(*[String]) throws TestException

      val thrown = intercept[Exception] {
        await(testInstance.getUserBatchLookupByKey(FakeLocationId))
      }

      val _ = thrown.getMessage shouldBe "Exception was thrown"
      val _ = mockDao.getUserBatchLookupByKey(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "get user batch lookup by code" should {
    "succeed" in {
      val _ = mockDao.getUserBatchLookupByCode(*[String]) returns Try(Some(dataUserBatchLookup))

      val maybeReturnedUser = await(testInstance.getUserBatchLookupByCode(FakeUserBatchCode))

      val _ = maybeReturnedUser.size shouldBe 1
      val _ = maybeReturnedUser.get shouldBe bizUserBatchLookup

      val _ = mockDao.getUserBatchLookupByCode(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail" in {
      val _ = mockDao.getUserBatchLookupByCode(*[String]) returns Try(None)

      val maybeReturnedUser = await(testInstance.getUserBatchLookupByCode(FakeUserBatchCode))

      val _ = maybeReturnedUser.size shouldBe 0

      val _ = mockDao.getUserBatchLookupByCode(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.getUserBatchLookupByCode(*[String]) throws TestException

      val thrown = intercept[Exception] {
        await(testInstance.getUserBatchLookupByCode(FakeLocationId))
      }

      val _ = thrown.getMessage shouldBe "Exception was thrown"
      val _ = mockDao.getUserBatchLookupByCode(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

private object UserBatchLookupRepositoryImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")
}

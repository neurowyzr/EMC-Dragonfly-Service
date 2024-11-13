package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.inject.TestMixin
import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeUserAccountId
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeRevInfo
import com.neurowyzr.nw.dragon.service.data.RevInfoDao
import com.neurowyzr.nw.dragon.service.data.impl.UserRepositoryImplTest.TestException
import com.neurowyzr.nw.dragon.service as root

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class RevInfoRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao      = mock[RevInfoDao]
  private val testInstance = new RevInfoRepositoryImpl(mockDao, pool)

  "createRevInfo" should {
    "return a user account with a new ID" in {
      val _ = mockDao.insertNewRevInfo(*[root.data.models.RevInfo]) returns Try(8845)

      val newRevInfoId = await(testInstance.createRevInfo(root.biz.impl.Fakes.FakeRevInfo))

      val _ = newRevInfoId shouldBe FakeRevInfo.modify(_.id).setTo(8845)
      val _ = mockDao.insertNewRevInfo(*[root.data.models.RevInfo]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "throw error" in {
    val _ = mockDao.insertNewRevInfo(*[root.data.models.RevInfo]) throws TestException

    val thrown = intercept[Exception] {
      await(testInstance.createRevInfo(root.biz.impl.Fakes.FakeRevInfo))
    }

    val _ = thrown.getMessage shouldBe "Exception was thrown"
    val _ = mockDao.insertNewRevInfo(*[root.data.models.RevInfo]) wasCalled once
    val _ = mockDao wasNever calledAgain
  }

  "get user account by user id and user batch is" should {
    "succeed" in {
      val _ = mockDao.getRevInfoById(*[Long]) returns Try(Seq(root.data.impl.Fakes.FakeRevInfo))

      val returnedRevInfo = await(testInstance.getRevInfoById(FakeUserAccountId))

      val _ = returnedRevInfo.size shouldBe 1
      val _ = returnedRevInfo.head shouldBe FakeRevInfo

      val _ = mockDao.getRevInfoById(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail" in {
      val _ = mockDao.getRevInfoById(*[Long]) returns Try(Seq.empty)

      val maybeReturnedUser = await(testInstance.getRevInfoById(FakeUserAccountId))

      val _ = maybeReturnedUser.size shouldBe 0

      val _ = mockDao.getRevInfoById(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.getRevInfoById(*[Long]) throws TestException

      val thrown = intercept[Exception] {
        await(testInstance.getRevInfoById(FakeUserAccountId))
      }

      val _ = thrown.getMessage shouldBe "Exception was thrown"
      val _ = mockDao.getRevInfoById(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

private object RevInfoRepositoryImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")
}

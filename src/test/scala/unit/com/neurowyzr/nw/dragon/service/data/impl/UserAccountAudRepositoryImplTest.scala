package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.inject.TestMixin
import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeUserAccountId
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeUserAccountAud
import com.neurowyzr.nw.dragon.service.data.UserAccountAudDao
import com.neurowyzr.nw.dragon.service.data.impl.UserRepositoryImplTest.TestException
import com.neurowyzr.nw.dragon.service as root

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class UserAccountAudRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao      = mock[UserAccountAudDao]
  private val testInstance = new UserAccountAudRepositoryImpl(mockDao, pool)

  "createUserAccountAud" should {
    "return a user account with a new ID" in {
      val _ = mockDao.insertNewUserAccountAud(*[root.data.models.UserAccountAud]) returns Try(456456L)

      val newUserAccountAudId = await(testInstance.createUserAccountAud(root.biz.impl.Fakes.FakeUserAccountAud))

      val _ = newUserAccountAudId shouldBe FakeUserAccountAud.modify(_.id).setTo(456456L)
      val _ = mockDao.insertNewUserAccountAud(*[root.data.models.UserAccountAud]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "throw error" in {
    val _ = mockDao.insertNewUserAccountAud(*[root.data.models.UserAccountAud]) throws TestException

    val thrown = intercept[Exception] {
      await(testInstance.createUserAccountAud(root.biz.impl.Fakes.FakeUserAccountAud))
    }

    val _ = thrown.getMessage shouldBe "Exception was thrown"
    val _ = mockDao.insertNewUserAccountAud(*[root.data.models.UserAccountAud]) wasCalled once
    val _ = mockDao wasNever calledAgain
  }

  "get user account by user id and user batch is" should {
    "succeed" in {
      val _ = mockDao.getUserAccountAudById(*[Long]) returns Try(Seq(root.data.impl.Fakes.FakeUserAccountAud))

      val returnedUserAccountAud = await(testInstance.getUserAccountAudById(FakeUserAccountId))

      val _ = returnedUserAccountAud.size shouldBe 1
      val _ = returnedUserAccountAud.head shouldBe FakeUserAccountAud

      val _ = mockDao.getUserAccountAudById(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail" in {
      val _ = mockDao.getUserAccountAudById(*[Long]) returns Try(Seq.empty)

      val maybeReturnedUser = await(testInstance.getUserAccountAudById(FakeUserAccountId))

      val _ = maybeReturnedUser.size shouldBe 0

      val _ = mockDao.getUserAccountAudById(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.getUserAccountAudById(*[Long]) throws TestException

      val thrown = intercept[Exception] {
        await(testInstance.getUserAccountAudById(FakeUserAccountId))
      }

      val _ = thrown.getMessage shouldBe "Exception was thrown"
      val _ = mockDao.getUserAccountAudById(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

private object UserAccountAudRepositoryImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")
}

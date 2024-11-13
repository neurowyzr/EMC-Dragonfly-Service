package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.inject.TestMixin
import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeNewUserId
import com.neurowyzr.nw.dragon.service.data.UserRoleDao
import com.neurowyzr.nw.dragon.service.data.impl.UserRepositoryImplTest.TestException
import com.neurowyzr.nw.dragon.service as root

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UserRoleRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao      = mock[UserRoleDao]
  private val testInstance = new UserRoleRepositoryImpl(mockDao, pool)

  "createUserRole" should {
    "successfully create a user role" in {
      val _ = mockDao.insertNewUserRole(*[root.data.models.UserRole]) returns Try(FakeNewUserId)

      val userRole = await(testInstance.createUserRole(root.biz.impl.Fakes.FakeUserRole))

      val _ = userRole shouldBe root.biz.impl.Fakes.FakeUserRole
      val _ = mockDao.insertNewUserRole(*[root.data.models.UserRole]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "handle errors when creating a user role" in {
      val _ = mockDao.insertNewUserRole(*[root.data.models.UserRole]) throws TestException

      val thrown = intercept[Exception] {
        await(testInstance.createUserRole(root.biz.impl.Fakes.FakeUserRole))
      }

      val _ = thrown.getMessage shouldBe "Exception was thrown"
      val _ = mockDao.insertNewUserRole(*[root.data.models.UserRole]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

private object UserRoleRepositoryImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")
}

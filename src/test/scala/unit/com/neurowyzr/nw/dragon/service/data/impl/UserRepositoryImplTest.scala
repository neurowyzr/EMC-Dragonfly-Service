package com.neurowyzr.nw.dragon.service.data.impl

import java.sql.SQLIntegrityConstraintViolationException

import com.twitter.inject.TestMixin
import com.twitter.util.{Throw, Try}

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeNewUserId
import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException
import com.neurowyzr.nw.dragon.service.data.UserDao
import com.neurowyzr.nw.dragon.service.data.impl.UserRepositoryImplTest.TestException
import com.neurowyzr.nw.dragon.service as root

import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class UserRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao      = mock[UserDao]
  private val testInstance = new UserRepositoryImpl(mockDao, pool)

  "get user by patient id" should {
    "succeed" in {
      val dataUser = root.data.models.User("fake-username",
                                           "fake-password",
                                           "fake-first-name",
                                           "fake-source",
                                           "fake-patient-id"
                                          )
      val dataUserWithPatientRef = dataUser.copy(maybeExternalPatientRef = Some("fake-patient-id"))
      val bizUser = root.biz.models.User("fake-username",
                                         "fake-password",
                                         "fake-first-name",
                                         "fake-source",
                                         "fake-patient-id"
                                        )
      val bizUserWithPatientRef = bizUser.copy(maybeExternalPatientRef = Some("fake-patient-id"))

      val _ = mockDao.getUserByExtPatientRef(*[String]) returns Try(Some(dataUserWithPatientRef))

      val maybeReturnedUser = await(testInstance.getUserByExternalPatientRef("fake-patient-id"))

      val _ = maybeReturnedUser.size shouldBe 1
      val _ = maybeReturnedUser.get shouldBe bizUserWithPatientRef

      val _ = mockDao.getUserByExtPatientRef(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail" in {
      val _ = mockDao.getUserByExtPatientRef(*[String]) returns Try(None)

      val maybeReturnedUser = await(testInstance.getUserByExternalPatientRef("fake-patient-id"))

      val _ = maybeReturnedUser.size shouldBe 0

      val _ = mockDao.getUserByExtPatientRef(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.getUserByExtPatientRef(*[String]) throws TestException

      val thrown = intercept[Exception] {
        await(testInstance.getUserByExternalPatientRef("fake-patient-id"))
      }

      val _ = thrown.getMessage shouldBe "Exception was thrown"
      val _ = mockDao.getUserByExtPatientRef(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "get user by source and patient id" should {
    "succeed" in {
      val dataUser = root.data.models.User("fake-username",
                                           "fake-password",
                                           "fake-first-name",
                                           "fake-source",
                                           "fake-patient-id"
                                          )
      val bizUser = root.biz.models.User("fake-username",
                                         "fake-password",
                                         "fake-first-name",
                                         "fake-source",
                                         "fake-patient-id"
                                        )

      val _ = mockDao.getUserBySourceAndExtPatientRef(*[String], *[String]) returns Try(Some(dataUser))

      val maybeReturnedUser = await(testInstance.getUserBySourceAndExternalPatientRef("fake-source", "fake-patient-id"))

      val _ = maybeReturnedUser.size shouldBe 1
      val _ = maybeReturnedUser.get shouldBe bizUser

      val _ = mockDao.getUserBySourceAndExtPatientRef(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail" in {
      val _ = mockDao.getUserBySourceAndExtPatientRef(*[String], *[String]) returns Try(None)

      val maybeReturnedUser = await(testInstance.getUserBySourceAndExternalPatientRef("fake-source", "fake-patient-id"))

      val _ = maybeReturnedUser.size shouldBe 0

      val _ = mockDao.getUserBySourceAndExtPatientRef(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.getUserBySourceAndExtPatientRef(*[String], *[String]) throws TestException

      val thrown = intercept[Exception] {
        await(testInstance.getUserBySourceAndExternalPatientRef("fake-source", "fake-patient-id"))
      }

      val _ = thrown.getMessage shouldBe "Exception was thrown"
      val _ = mockDao.getUserBySourceAndExtPatientRef(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "get user by username" should {
    "succeed" in {
      val dataUser = root.data.models.User("fake-username",
                                           "fake-password",
                                           "fake-first-name",
                                           "fake-source",
                                           "fake-patient-id"
                                          )
      val bizUser = root.biz.models.User("fake-username",
                                         "fake-password",
                                         "fake-first-name",
                                         "fake-source",
                                         "fake-patient-id"
                                        )

      val _ = mockDao.getUserByUsername(*[String]) returns Try(Some(dataUser))

      val maybeReturnedUser = await(testInstance.getUserByUsername("fake-username"))

      val _ = maybeReturnedUser.size shouldBe 1
      val _ = maybeReturnedUser.get shouldBe bizUser

      val _ = mockDao.getUserByUsername(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail" in {
      val _ = mockDao.getUserByUsername(*[String]) returns Try(None)

      val maybeReturnedUser = await(testInstance.getUserByUsername("fake-username"))

      val _ = maybeReturnedUser.size shouldBe 0

      val _ = mockDao.getUserByUsername(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.getUserByUsername(*[String]) throws TestException

      val thrown = intercept[Exception] {
        await(testInstance.getUserByUsername("fake-username"))
      }

      val _ = thrown.getMessage shouldBe "Exception was thrown"
      val _ = mockDao.getUserByUsername(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "get user by id" should {
    "succeed" in {
      val dataUser = root.data.models.User("fake-username",
                                           "fake-password",
                                           "fake-first-name",
                                           "fake-source",
                                           "fake-patient-id"
                                          )
      val bizUser = root.biz.models.User("fake-username",
                                         "fake-password",
                                         "fake-first-name",
                                         "fake-source",
                                         "fake-patient-id"
                                        )

      val _ = mockDao.getUserById(*[Long]) returns Try(Some(dataUser))

      val maybeReturnedUser = await(testInstance.getUserById(FakeNewUserId))

      val _ = maybeReturnedUser.size shouldBe 1
      val _ = maybeReturnedUser.get shouldBe bizUser

      val _ = mockDao.getUserById(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail" in {
      val _ = mockDao.getUserById(*[Long]) returns Try(None)

      val maybeReturnedUser = await(testInstance.getUserById(FakeNewUserId))

      val _ = maybeReturnedUser.size shouldBe 0

      val _ = mockDao.getUserById(FakeNewUserId) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.getUserById(*[Long]) throws TestException

      val thrown = intercept[Exception] {
        await(testInstance.getUserById(FakeNewUserId))
      }

      val _ = thrown.getMessage shouldBe "Exception was thrown"
      val _ = mockDao.getUserById(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "createUser" should {
    "return a user with a new ID" in {
      val _ = mockDao.insertNewUser(*[root.data.models.User]) returns Try(FakeNewUserId)

      val newUser = await(testInstance.createUser(root.biz.impl.Fakes.FakeUser))

      val _ =
        newUser shouldBe root.biz.impl.Fakes.FakeUser
          .modify(_.id)
          .setTo(FakeNewUserId)
          .modify(_.maybeUtcCreatedAt)
          .setTo(newUser.maybeUtcCreatedAt)
      val _ = mockDao.insertNewUser(*[root.data.models.User]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw exception is user is already created" in {
      val _ =
        mockDao.insertNewUser(*[root.data.models.User]) returns Throw(
          new SQLIntegrityConstraintViolationException("error-message")
        )

      val thrown = intercept[BizException] {
        await(testInstance.createUser(root.biz.impl.Fakes.FakeUser))
      }

      val _ = thrown.getMessage shouldBe "Session id: fake-user-name is already created."
      val _ = mockDao.insertNewUser(*[root.data.models.User]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

  }

  "updateUser" should {
    "return the id of the user" in {
      val _ = mockDao.updateUser(*[root.data.models.User]) returns Try(FakeNewUserId)

      val userId = await(testInstance.updateUser(root.biz.impl.Fakes.FakeUser))

      val _ = userId shouldBe FakeNewUserId
      val _ = mockDao.updateUser(*[root.data.models.User]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "deleteUserByUsername" should {
    "delete user succeed" in {
      val _ = mockDao.deleteUserByUsername(*[String]) returns Try(1)

      val res = await(testInstance.deleteUserByUsername(root.biz.impl.Fakes.FakeUser.username))
      res shouldBe true

      val _ = mockDao.deleteUserByUsername(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
    "delete user did not take place due to user not found" in {
      val _ = mockDao.deleteUserByUsername(*[String]) returns Try(0)

      val res = await(testInstance.deleteUserByUsername(root.biz.impl.Fakes.FakeUser.username))
      res shouldBe false

      val _ = mockDao.deleteUserByUsername(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

private object UserRepositoryImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")
}

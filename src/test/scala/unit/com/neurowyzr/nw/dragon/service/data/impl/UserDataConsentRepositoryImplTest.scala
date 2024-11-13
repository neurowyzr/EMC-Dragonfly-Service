package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.inject.TestMixin
import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeNewUserId
import com.neurowyzr.nw.dragon.service.data.UserDataConsentDao
import com.neurowyzr.nw.dragon.service.data.impl.Fakes.FakeUserConsent
import com.neurowyzr.nw.dragon.service as root

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class UserDataConsentRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao      = mock[UserDataConsentDao]
  private val testInstance = new UserDataConsentRepositoryImpl(mockDao, pool)

  "createUserConsent" should {
    "return created user consent" in {
      val _ = mockDao.insertConsent(*[root.data.models.UserDataConsent]) returns Try(FakeNewUserId.toString)

      val userId = await(testInstance.createUserConsent(root.biz.impl.Fakes.FakeUserDataConsent))

      val _ = userId shouldBe FakeNewUserId
      val _ = mockDao.insertConsent(*[root.data.models.UserDataConsent]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "deleteUserConsentByUserId" should {
    "delete created user consent" in {
      val _ = mockDao.revokeConsentByUserId(*[Long]) returns Try(FakeNewUserId.toString)

      val userId = await(testInstance.deleteUserConsentByUserId(FakeNewUserId))

      val _ = userId shouldBe FakeNewUserId
      val _ = mockDao.revokeConsentByUserId(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "getUserConsentByUserId" should {
    "return user consent" in {
      val _ = mockDao.getUserConsentByUserId(*[Long]) returns Try(Some(FakeUserConsent))

      val consent = await(testInstance.getUserConsentByUserId(FakeUserConsent.userId))

      val _ = consent.isDefined shouldBe true
      val _ = consent.map(_.userId) shouldBe Some(FakeUserConsent.userId)
      val _ = mockDao.getUserConsentByUserId(*[Long]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

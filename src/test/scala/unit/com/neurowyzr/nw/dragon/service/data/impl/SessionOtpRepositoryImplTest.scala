package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.inject.TestMixin
import com.twitter.util.{Future, Try}

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeSessionId, FakeValidEmail}
import com.neurowyzr.nw.dragon.service.data.SessionOtpDao
import com.neurowyzr.nw.dragon.service.data.impl.Fakes.FakeSessionOtp
import com.neurowyzr.nw.dragon.service.data.impl.SessionOtpRepositoryImplTest.TestException
import com.neurowyzr.nw.dragon.service.data.models.SessionOtp
import com.neurowyzr.nw.dragon.service.utils.context.EncryptionUtil
import com.neurowyzr.nw.dragon.service as root

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class SessionOtpRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao: SessionOtpDao = mock[SessionOtpDao]
  private val testInstance           = new SessionOtpRepositoryImpl(mockDao, pool)

  "get session otp by session id and email" should {
    "succeed" in {
      val _ = mockDao.getSessionOtp(*[String], *[String]) returns Try(Some(FakeSessionOtp))

      val maybeSessionOtp = await(testInstance.getSessionOtp(FakeSessionId, FakeValidEmail))

      val _ = maybeSessionOtp.size shouldBe 1
      val _ = maybeSessionOtp.value shouldBe root.biz.impl.Fakes.FakeSessionOtp
      val _ = mockDao.getSessionOtp(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail" in {
      val _ = mockDao.getSessionOtp(*[String], *[String]) returns Try(None)

      val maybeSessionOtp = await(testInstance.getSessionOtp(FakeSessionId, FakeValidEmail))

      val _ = maybeSessionOtp.size shouldBe 0
      val _ = mockDao.getSessionOtp(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.getSessionOtp(*[String], *[String]) throws TestException

      val thrown = intercept[Exception](
        await(
          testInstance.getSessionOtp(FakeSessionId, FakeValidEmail)
        )
      )

      val _ = thrown shouldBe TestException
      val _ = mockDao.getSessionOtp(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "update session otp" should {
    "succeed" in {
      val _ = mockDao.updateSessionOtp(*[SessionOtp]) returns Try(12345L)

      val sessionOtpId = await(testInstance.updateSessionOtp(root.biz.impl.Fakes.FakeSessionOtp))

      val _ = sessionOtpId shouldBe 12345L
      val _ = mockDao.updateSessionOtp(*[SessionOtp]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.updateSessionOtp(*[SessionOtp]) throws TestException

      val thrown = intercept[Exception](
        await(
          testInstance.updateSessionOtp(root.biz.impl.Fakes.FakeSessionOtp)
        )
      )

      val _ = thrown shouldBe TestException
      val _ = mockDao.updateSessionOtp(*[SessionOtp]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "insert session otp" should {
    "succeed" in {
      val _ = mockDao.insertSessionOtp(*[SessionOtp]) returns Try(6789L)

      val sessionOtpId = await(testInstance.insertSessionOtp(root.biz.impl.Fakes.FakeSessionOtp))

      val _ = sessionOtpId shouldBe root.biz.impl.Fakes.FakeSessionOtp.copy(id = 6789L)
      val _ = mockDao.insertSessionOtp(*[SessionOtp]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "throw error" in {
      val _ = mockDao.insertSessionOtp(*[SessionOtp]) throws TestException

      val thrown = intercept[Exception](
        await(
          testInstance.insertSessionOtp(root.biz.impl.Fakes.FakeSessionOtp)
        )
      )

      val _ = thrown shouldBe TestException
      val _ = mockDao.insertSessionOtp(*[SessionOtp]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "invalid session otp" should {
    "succeed" in {
      val _ = mockDao.invalidateSessionOtp(*[String], *[String]) returns Try(6789L)

      val res = await(testInstance.invalidateSessionOtp(FakeSessionId, FakeValidEmail))

      val _ = res shouldBe ()
      val _ = mockDao.invalidateSessionOtp(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

private object SessionOtpRepositoryImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")
}

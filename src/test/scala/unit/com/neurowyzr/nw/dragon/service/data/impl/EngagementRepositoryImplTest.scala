package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.inject.TestMixin
import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeUserBatchCode
import com.neurowyzr.nw.dragon.service.data.EngagementDao
import com.neurowyzr.nw.dragon.service.data.impl.Fakes.FakeEngagement
import com.neurowyzr.nw.dragon.service.data.impl.UserRepositoryImplTest.TestException
import com.neurowyzr.nw.dragon.service as root

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class EngagementRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao: EngagementDao = mock[EngagementDao]
  private val testInstance           = new EngagementRepositoryImpl(mockDao, pool)

  "get engagement by user batch code" should {
    "succeed" in {
      val _ = mockDao.getEngagementByUserBatchCode(*[String]) returns Try(Some(FakeEngagement))

      val maybeEngagement = await(testInstance.getEngagementByUserBatchCode(FakeUserBatchCode))

      val _ = maybeEngagement.value shouldBe root.biz.impl.Fakes.FakeEngagement
      val _ = mockDao.getEngagementByUserBatchCode(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown for insertion issue" in {
      val _ = mockDao.getEngagementByUserBatchCode(*[String]) throws TestException

      val exception = intercept[Exception](
        await(
          testInstance.getEngagementByUserBatchCode(FakeUserBatchCode)
        )
      )

      val _ = exception shouldBe TestException
      val _ = mockDao.getEngagementByUserBatchCode(*[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

private object EngagementRepositoryImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")
}

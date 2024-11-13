package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.inject.TestMixin
import com.twitter.util.{Throw, Try}

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeUserBatchCode, FakeUserName}
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeTestSession
import com.neurowyzr.nw.dragon.service.data.TestSessionDao
import com.neurowyzr.nw.dragon.service.data.impl.Fakes.FakeTestSessionsWithSessionId
import com.neurowyzr.nw.dragon.service.data.impl.UserRepositoryImplTest.TestException
import com.neurowyzr.nw.dragon.service.data.models.TestSession

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class TestSessionRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao: TestSessionDao = mock[TestSessionDao]
  private val testInstance            = new TestSessionRepositoryImpl(mockDao, pool)

  "insert new test session " should {
    "succeed" in {
      val _ = mockDao.insertNewTestSession(*[TestSession]) returns Try(FakeTestSession.id)

      val testSession = await(testInstance.insert(FakeTestSession))

      val _ = testSession shouldBe FakeTestSession
      val _ = mockDao.insertNewTestSession(*[TestSession]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }

    "fail when exception is thrown for insertion issue" in {
      val _ = mockDao.insertNewTestSession(*[TestSession]) returns Throw(TestException)

      val exception = intercept[Exception](
        await(
          testInstance.insert(FakeTestSession)
        )
      )

      val _ = mockDao.insertNewTestSession(*[TestSession]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

  "get test session by username and user batch code" should {
    "succeed when username and user batch code are found" in {
      val _ =
        mockDao.getTestSessionsByUsernameAndUserBatch(*[String], *[String]) returns Try(FakeTestSessionsWithSessionId)

      val testSessions = await(testInstance.getTestSessionsByUsernameAndUserBatch(FakeUserName, FakeUserBatchCode))

      val _ = testSessions.head.sessionId shouldBe FakeTestSessionsWithSessionId.head._2
      val _ = mockDao.getTestSessionsByUsernameAndUserBatch(*[String], *[String]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

private object TestSessionRepositoryImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")
}

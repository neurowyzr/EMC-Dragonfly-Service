package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.inject.TestMixin
import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeNewEpisodeRef
import com.neurowyzr.nw.dragon.service.data.UserSurveyDao
import com.neurowyzr.nw.dragon.service as root

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UserSurveyRepositoryImplTest
    extends AnyWordSpec with TestMixin with IdiomaticMockito with ResetMocksAfterEachTest with Matchers
    with OptionValues {

  private val mockDao      = mock[UserSurveyDao]
  private val testInstance = new UserSurveyRepositoryImpl(mockDao, pool)

  "createUserSurvey" should {
    "return created user survey" in {
      val _ = mockDao.insertSurveySelections(*[root.data.models.UserSurvey]) returns Try(FakeNewEpisodeRef.toString)

      val sessionId = await(testInstance.createUserSurvey(root.biz.impl.Fakes.FakeUserSurvey))

      val _ = sessionId shouldBe root.biz.impl.Fakes.FakeUserSurvey.sessionId
      val _ = mockDao.insertSurveySelections(*[root.data.models.UserSurvey]) wasCalled once
      val _ = mockDao wasNever calledAgain
    }
  }

}

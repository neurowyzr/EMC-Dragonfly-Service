package com.neurowyzr.nw.dragon.service.biz.impl

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.{FakeUserBatchCode, FakeUserName}
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.{FakeTestSessions, FakeTestSessionWithSessionId}
import com.neurowyzr.nw.dragon.service.data.TestSessionRepository

import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ScoreServiceImplTest
    extends AnyWordSpecLike with IdiomaticMockito with ArgumentMatchersSugar with Matchers with ResetMocksAfterEachTest
    with OptionValues {

  private val testSessionRepo = mock[TestSessionRepository]
  private val testInstance    = new ScoreServiceImpl(testSessionRepo)

  "getScores" when {
    "completed test session exists" should {
      "return extracted scores" in {
        val _ =
          testSessionRepo.getTestSessionsByUsernameAndUserBatch(*[String], *[String]) returns Future.value(
            FakeTestSessions
          )
        val username      = FakeUserName
        val userBatchCode = FakeUserBatchCode

        val maybeResult = Await.result(testInstance.getScores(username, userBatchCode), 1.second)
        maybeResult.isDefined shouldBe true
        maybeResult.map { result =>
          result.latestSessionId shouldBe "fakeRef2"
        }
      }
    }
    "no completed test sessions exists" should {
      "return None" in {
        val _ =
          testSessionRepo.getTestSessionsByUsernameAndUserBatch(*[String], *[String]) returns Future.value(
            Seq.empty
          )
        val username      = FakeUserName
        val userBatchCode = FakeUserBatchCode

        val result = Await.result(testInstance.getScores(username, userBatchCode), 1.second)
        result.isEmpty shouldBe true
      }
    }
  }

  "generateGetScoresResponse" should {
    "return -1 for overall latest score if zScore does not exist in" in {
      val res = testInstance.generateGetScoresResponse(FakeTestSessionWithSessionId, Map.empty)
      res.overallLatestScore shouldBe -1
    }
  }

}

package com.neurowyzr.nw.dragon.service.biz.impl

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finagle.http.{Response, Status}
import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.*
import com.neurowyzr.nw.dragon.service.biz.exceptions.*
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.*
import com.neurowyzr.nw.dragon.service.biz.models.*
import com.neurowyzr.nw.dragon.service.biz.models.UserModels.LatestReportParams
import com.neurowyzr.nw.dragon.service.clients.CoreHttpClient
import com.neurowyzr.nw.dragon.service.data.*

import org.mockito.ArgumentMatchersSugar
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class UserServiceImplTest
    extends AnyWordSpecLike with IdiomaticMockito with ArgumentMatchersSugar with Matchers with ResetMocksAfterEachTest
    with OptionValues {

  private val userRepo            = mock[UserRepository]
  private val userDataConsentRepo = mock[UserDataConsentRepository]
  private val coreHttpClient      = mock[CoreHttpClient]
  private val episodeRepo         = mock[EpisodeRepository]

  private val testInstance = new UserServiceImpl(userRepo, userDataConsentRepo, coreHttpClient, episodeRepo)

  "updateUser" when {
    "user is found" should {
      "update user" in {
        val captor = ArgCaptor[User]
        val _      = userRepo.getUserByUsername(FakeSessionId) returns Future.value(Some(FakeUser))
        val _      = userRepo.updateUser(captor) returns Future.value(1L)

        val _ = Await.result(testInstance.updateUser(FakeUpdateUserParams), 1.second)
        captor.value.maybeDateOfBirth.map(dobString => dobString shouldBe FakeBirthYear + "-01-01 00:00:00")

        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userRepo.updateUser(*[User]) wasCalled once
        val _ = userRepo wasNever calledAgain
      }
    }
    "user is not found" should {
      "do nothing" in {
        val _ = userRepo.getUserByUsername(FakeSessionId) returns Future.None
        val _ = userRepo.updateUser(*[User]) returns Future.value(1L)

        val thrown = intercept[UserNotFoundException] {
          Await.result(testInstance.updateUser(FakeUpdateUserParams), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"User '$FakeSessionId' does not exist"

        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userRepo.updateUser(*[User]) wasNever called
        val _ = userRepo wasNever calledAgain
      }
    }

  }

  "createUserConsent" when {
    "adding user consent" should {
      "create user consent" in {
        val captor = ArgCaptor[UserDataConsent]
        val _      = userRepo.getUserByUsername(*[String]) returns Future.value(Some(FakeUser))
        val _      = userDataConsentRepo.createUserConsent(captor) returns Future.value(1L)

        val _ = Await.result(testInstance.createUserConsent(FakeCreateUserDataConsentParams), 1.second)
        captor.value.userId shouldBe FakeUser.id

        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userDataConsentRepo.createUserConsent(*[UserDataConsent]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = userDataConsentRepo wasNever calledAgain
      }
    }
    "user is not found" should {
      "do nothing" in {
        val _ = userRepo.getUserByUsername(*) returns Future.None
        val _ = userRepo.updateUser(*[User]) returns Future.value(1L)

        val thrown = intercept[UserNotFoundException] {
          Await.result(testInstance.createUserConsent(FakeCreateUserDataConsentParams), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"User '${FakeCreateUserDataConsentParams.email}' does not exist"

        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userRepo.updateUser(*[User]) wasNever called
        val _ = userRepo wasNever calledAgain
      }
    }
  }

  "deleteUserByUsername" when {
    "delete user" should {
      "succeed" in {
        val _ = userRepo.deleteUserByUsername(*[String]) returns Future.True

        val _ = Await.result(testInstance.deleteUserByUsername(FakeUserName), 1.second)

        val _ = userRepo.deleteUserByUsername(*[String]) wasCalled once
        val _ = userRepo wasNever calledAgain
      }
    }
  }

  "deleteUserConsentByUserId" when {
    "user exists" should {
      "delete user consent" in {
        val _ = userRepo.getUserByUsername(*[String]) returns Future.value(Some(FakeUser))
        val _ = userDataConsentRepo.deleteUserConsentByUserId(*[Long]) returns Future.value(1L)

        val _ = Await.result(testInstance.deleteUserConsentByUsername(FakeUserName), 1.second)

        val _ = userDataConsentRepo.deleteUserConsentByUserId(*[Long]) wasCalled once
        val _ = userDataConsentRepo wasNever calledAgain
        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userRepo wasNever calledAgain
      }
    }
    "user does not exist" should {
      "do nothing" in {
        val _ = userRepo.getUserByUsername(*) returns Future.None

        val thrown = intercept[UserNotFoundException] {
          Await.result(testInstance.deleteUserConsentByUsername(FakeUserName), 1.second)
        }
        val _ = thrown.getMessage shouldBe s"User '$FakeUserName' does not exist"

        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = userDataConsentRepo wasNever called
      }
    }
  }

  "getUserByUsername" when {
    "user exists and consent exists" should {
      "return user" in {
        val _ = userRepo.getUserByUsername(*[String]) returns Future.value(Some(FakeUser))
        val _ = userDataConsentRepo.getUserConsentByUserId(*[Long]) returns Future.value(Some(FakeUserDataConsent))

        val user = Await.result(testInstance.getUserByUsername(FakeUser.username), 1.second)
        user.email shouldBe FakeUser.username
        user.name shouldBe FakeUser.firstName
        user.isDataConsent shouldBe FakeUserDataConsent.isConsent

        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userDataConsentRepo.getUserConsentByUserId(*[Long]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = userDataConsentRepo wasNever calledAgain
      }
    }
    "user exists but consent not found" should {
      "throw error" in {
        val _ = userRepo.getUserByUsername(*[String]) returns Future.value(Some(FakeUser))
        val _ = userDataConsentRepo.getUserConsentByUserId(*[Long]) returns Future.None

        val thrown = intercept[UserConsentNotFoundException] {
          Await.result(testInstance.getUserByUsername(FakeUser.username), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"Data consent for user '$FakeUserName' does not exist"

        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userDataConsentRepo.getUserConsentByUserId(*[Long]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = userDataConsentRepo wasNever calledAgain
      }
    }
    "user does not exist" should {
      "throw error" in {
        val _ = userRepo.getUserByUsername(*[String]) returns Future.None

        val thrown = intercept[UserNotFoundException] {
          Await.result(testInstance.getUserByUsername(FakeUser.username), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"User '$FakeUserName' does not exist"

        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = userDataConsentRepo wasNever called
      }
    }
  }

  "sendLatestReport" should {
    "successfully send a report when data is available" in {
      val httpResponse = mock[Response]
      httpResponse.status returns Status.Ok
      httpResponse.contentString returns "Report sent successfully"

      episodeRepo.getLatestCompletedTestSessionsByUsername(*[String]) returns Future.value(
        Some((FakeEpisodeAlpha, FakeTestSession))
      )
      coreHttpClient.sendLatestReport(*[LatestReportParams]) returns Future.value(httpResponse)

      val link = Await.result(testInstance.sendLatestReport(FakeUserBatchCode, FakeUserName), 1.second)

      link shouldBe "Report sent successfully"
      val _ = episodeRepo.getLatestCompletedTestSessionsByUsername(*[String]) wasCalled once
      val _ = coreHttpClient.sendLatestReport(*[LatestReportParams]) wasCalled once
      val _ = episodeRepo wasNever calledAgain
      val _ = coreHttpClient wasNever calledAgain
    }

    "throw ReportNotFoundException if no session data is available" in {
      episodeRepo.getLatestCompletedTestSessionsByUsername(*[String]) returns Future.None

      val thrown = intercept[ReportNotFoundException] {
        Await.result(testInstance.sendLatestReport(FakeUserBatchCode, FakeUserName), 1.second)
      }

      val _ = thrown.getMessage shouldBe s"Latest completed test session not found for: $FakeUserName"

      val _ = episodeRepo.getLatestCompletedTestSessionsByUsername(*[String]) wasCalled once
      val _ = episodeRepo wasNever calledAgain
    }

    "throw ReportNotFoundException if report sending fails" in {
      val httpResponse = mock[Response]
      httpResponse.status returns Status.NotFound

      episodeRepo.getLatestCompletedTestSessionsByUsername(*[String]) returns Future.value(
        Some((FakeEpisodeAlpha, FakeTestSession))
      )
      coreHttpClient.sendLatestReport(*[LatestReportParams]) returns Future.value(httpResponse)

      val thrown = intercept[ReportNotFoundException] {
        Await.result(testInstance.sendLatestReport(FakeUserBatchCode, FakeUserName), 1.second)
      }

      val _ = thrown.getMessage shouldBe s"Report for $FakeUserName not found"
      val _ = episodeRepo.getLatestCompletedTestSessionsByUsername(*[String]) wasCalled once
      val _ = coreHttpClient.sendLatestReport(*[LatestReportParams]) wasCalled once
      val _ = episodeRepo wasNever calledAgain
      val _ = coreHttpClient wasNever calledAgain
    }
  }

}

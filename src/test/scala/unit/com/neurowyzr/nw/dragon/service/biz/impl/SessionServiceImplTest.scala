package com.neurowyzr.nw.dragon.service.biz.impl

import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneId}

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.util.{Await, Future, Throw, Try}

import com.neurowyzr.nw.dragon.service.SharedFakes.*
import com.neurowyzr.nw.dragon.service.biz.exceptions.*
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.*
import com.neurowyzr.nw.dragon.service.biz.impl.SessionServiceImplTest.FakePayload
import com.neurowyzr.nw.dragon.service.biz.models.*
import com.neurowyzr.nw.dragon.service.biz.models.Defaults.DefaultLongId
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.*
import com.neurowyzr.nw.dragon.service.biz.models.UserSurveyModels.{CreateUserSurveyParams, UserSurvey}
import com.neurowyzr.nw.dragon.service.clients.CoreHttpClient
import com.neurowyzr.nw.dragon.service.data.*
import com.neurowyzr.nw.dragon.service.mq.{EmailPublisher, SelfPublisher}
import com.neurowyzr.nw.dragon.service.utils.context.JwtUtil

import org.mockito.ArgumentMatchersSugar
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.wordspec.AnyWordSpecLike

class SessionServiceImplTest
    extends AnyWordSpecLike with IdiomaticMockito with ArgumentMatchersSugar with Matchers with ResetMocksAfterEachTest
    with OptionValues {

  private val engagementRepo     = mock[EngagementRepository]
  private val userBatchRepo      = mock[UserBatchRepository]
  private val episodeRepo        = mock[EpisodeRepository]
  private val userRepo           = mock[UserRepository]
  private val userAccountRepo    = mock[UserAccountRepository]
  private val revInfoRepo        = mock[RevInfoRepository]
  private val userAccountAudRepo = mock[UserAccountAudRepository]
  private val userRoleRepo       = mock[UserRoleRepository]
  private val userSurveyRepo     = mock[UserSurveyRepository]
  private val sessionOtpRepo     = mock[SessionOtpRepository]
  private val dbfsConfig         = FakeDbfsConfig
  private val emailPublisher     = mock[EmailPublisher]
  private val selfPublisher      = mock[SelfPublisher]
  private val coreHttpClient     = mock[CoreHttpClient]
  private val jsonMapper         = (new ScalaObjectMapperModule).camelCaseObjectMapper

  private val testInstance = {
    new SessionServiceImpl(
      engagementRepo,
      userBatchRepo,
      episodeRepo,
      userRepo,
      userAccountRepo,
      revInfoRepo,
      userAccountAudRepo,
      userRoleRepo,
      sessionOtpRepo,
      dbfsConfig,
      emailPublisher,
      selfPublisher,
      coreHttpClient,
      userSurveyRepo,
      jsonMapper,
      "Asia/Singapore"
    )
  }

  "validatePatientIdAndEpisodeId" when {

    "patient_id and uid is different" should {
      "fail with BizException" in {
        val params =
          new EnqueueTestSessionParams(
            requestId = "fake-request-id",
            patientId = FakePatientIdAlpha,
            episodeId = FakeEpisodeIdAlpha.toString,
            patientRef = FakePatientIdBravo,
            episodeRef = FakeEpisodeIdAlpha.toString,
            locationId = "fake-location-id",
            firstName = "fake-first-name",
            lastName = "fake-last-name",
            birthDate = "fake-dob",
            gender = "fake-gender",
            maybeEmail = Some("fake-email"),
            maybeMobileNumber = Some(123456790)
          )

        val result = testInstance.validatePatientIdAndEpisodeId(params)
        val expected = Throw(
          BizException(s"Expecting patient id: $FakePatientIdAlpha but received $FakePatientIdBravo.")
        )

        result shouldBe expected
      }
    }

    "episode_id and ahc_number is different" should {
      "fail with BizException" in {
        val params =
          new EnqueueTestSessionParams(
            requestId = "fake-request-id",
            patientId = FakePatientIdAlpha,
            episodeId = FakeEpisodeIdBravo.toString,
            patientRef = FakePatientIdAlpha,
            episodeRef = FakeEpisodeIdAlpha.toString,
            locationId = "fake-location-id",
            firstName = "fake-first-name",
            lastName = "fake-last-name",
            birthDate = "fake-dob",
            gender = "fake-gender",
            maybeEmail = Some("fake-email"),
            maybeMobileNumber = Some(123456790)
          )
        val expected = Throw(
          BizException(
            s"Expecting episode id: ${FakeEpisodeIdBravo.toString} but received ${FakeEpisodeIdAlpha.toString}."
          )
        )

        val result = testInstance.validatePatientIdAndEpisodeId(params)

        result shouldBe expected
      }
    }

    "valid patient id and episode id" should {
      "succeed" in {
        val params =
          new EnqueueTestSessionParams(
            requestId = "fake-request-id",
            patientId = FakePatientIdAlpha,
            episodeId = FakeEpisodeIdAlpha.toString,
            patientRef = FakePatientIdAlpha,
            episodeRef = FakeEpisodeIdAlpha.toString,
            locationId = "fake-location-id",
            firstName = "fake-first-name",
            lastName = "fake-last-name",
            birthDate = "fake-dob",
            gender = "fake-gender",
            maybeEmail = Some("fake-email"),
            maybeMobileNumber = Some(123456790)
          )

        val expected = Try.Unit

        val result = testInstance.validatePatientIdAndEpisodeId(params)

        result shouldBe expected
      }
    }
  }

  "createClientMagicLink" when {

    "task context and payload is valid" should {
      "succeed" in {
        val _ = selfPublisher.publishTestSessionMessage(*[EnqueueTestSessionParams]) returns Future.value("")

        val result = Await.result(testInstance.enqueueTestSession(FakePayload))

        val _ = result shouldBe ()
        val _ = selfPublisher.publishTestSessionMessage(*[EnqueueTestSessionParams]) wasCalled once
        val _ = selfPublisher wasNever calledAgain
      }
    }
  }

  "createSession" when {

    "user batch code is not found" should {
      "fail with BizException" in {
        val _      = userBatchRepo.getUserBatchByCode(*[String]) returns Future.value(None)
        val params = CreateSessionParams(FakeSessionId, FakeUserBatchCode, FakeValidEmail)

        val thrown = intercept[BizException] {
          Await.result(testInstance.createSession(params), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"User batch code: $FakeUserBatchCode not found."
        val _ = userBatchRepo.getUserBatchByCode(*[String]) wasCalled once
        val _ = userBatchRepo wasNever calledAgain
      }
    }

    "engagement does not exist for user batch code" should {
      "fail with BizException" in {
        val _      = userBatchRepo.getUserBatchByCode(*[String]) returns Future.value(Some(FakeUserBatch))
        val _      = engagementRepo.getEngagementByUserBatchCode(*[String]) returns Future.None
        val params = CreateSessionParams(FakeSessionId, FakeUserBatchCode, FakeValidEmail)

        val thrown = intercept[BizException] {
          Await.result(testInstance.createSession(params), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"Engagement doesn't exist for this user batch code: $FakeUserBatchCode"
        val _ = userBatchRepo.getUserBatchByCode(*[String]) wasCalled once
        val _ = engagementRepo.getEngagementByUserBatchCode(*[String]) wasCalled once
        val _ = userBatchRepo wasNever calledAgain
        val _ = engagementRepo wasNever calledAgain
      }
    }

    "user not found for given email" should {
      "fail with BizException" in {
        val _      = userBatchRepo.getUserBatchByCode(*[String]) returns Future.value(Some(FakeUserBatch))
        val _      = engagementRepo.getEngagementByUserBatchCode(*[String]) returns Future.value(Some(FakeEngagement))
        val _      = userRepo.getUserByUsername(*[String]) returns Future.value(None)
        val params = CreateSessionParams(FakeSessionId, FakeUserBatchCode, FakeValidEmail)

        val thrown = intercept[BizException] {
          Await.result(testInstance.createSession(params), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"User not found for email: $FakeValidEmail"
        val _ = userBatchRepo.getUserBatchByCode(*[String]) wasCalled once
        val _ = engagementRepo.getEngagementByUserBatchCode(*[String]) wasCalled once
        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userBatchRepo wasNever calledAgain
        val _ = engagementRepo wasNever calledAgain
        val _ = userRepo wasNever calledAgain
      }
    }

    "user batch code, engagement, and user are valid" should {
      "create a new session and return the magic link" in {
        val _ = userBatchRepo.getUserBatchByCode(*[String]) returns Future.value(Some(FakeUserBatch))
        val _ = engagementRepo.getEngagementByUserBatchCode(*[String]) returns Future.value(Some(FakeEngagement))
        val _ = userRepo.getUserByUsername(*[String]) returns Future.value(Some(FakeUser))
        val _ =
          userAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) returns Future.value(
            Some(FakeUserAccount)
          )
        val _ = userAccountAudRepo.getUserAccountAudById(*[Long]) returns Future.value(Seq(FakeUserAccountAud))
        val _ = revInfoRepo.createRevInfo(*[RevInfo]) returns Future.value(FakeRevInfo)
        val _ = userAccountAudRepo.createUserAccountAud(*[UserAccountAud]) returns Future.value(FakeUserAccountAud)
        val _ =
          coreHttpClient.getCurrentTestSession(*[CurrentTestSession]) returns Future.value(
            FakeMagicLinkTestSessionDetail
          )
        val _ =
          episodeRepo.insertEpisodeAndTestSession(*[Episode], *[TestSession]) returns Future.value(
            (FakeNewEpisode, FakeTestSession)
          )
        val params = CreateSessionParams(FakeSessionId, FakeUserBatchCode, FakeValidEmail)

        val result = Await.result(testInstance.createSession(params), 1.second)

        val _ = result shouldBe dbfsConfig.magicLinkPath + "/" + FakeSessionId
        val _ = userBatchRepo.getUserBatchByCode(*[String]) wasCalled once
        val _ = engagementRepo.getEngagementByUserBatchCode(*[String]) wasCalled once
        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
        val _ = userAccountAudRepo.getUserAccountAudById(*[Long]) wasCalled once
        val _ = revInfoRepo.createRevInfo(*[RevInfo]) wasCalled once
        val _ = userAccountAudRepo.createUserAccountAud(*[UserAccountAud]) wasCalled once
        val _ = coreHttpClient.getCurrentTestSession(*[CurrentTestSession]) wasCalled once
        val _ = episodeRepo.insertEpisodeAndTestSession(*[Episode], *[TestSession]) wasCalled once
        val _ = userBatchRepo wasNever calledAgain
        val _ = engagementRepo wasNever calledAgain
        val _ = userRepo wasNever calledAgain
        val _ = userAccountRepo wasNever calledAgain
        val _ = userAccountAudRepo wasNever calledAgain
        val _ = revInfoRepo wasNever calledAgain
        val _ = userAccountAudRepo wasNever calledAgain
        val _ = coreHttpClient wasNever calledAgain
        val _ = episodeRepo wasNever calledAgain
      }
    }

  }

  "createUserAndSession" when {
    "user batch code is invalid" should {
      "fail" in {
        val _      = userBatchRepo.getUserBatchByCode(*[String]) returns Future.None
        val params = FakeCreateUserSessionParams

        val thrown = intercept[BizException] {
          Await.result(testInstance.createUserAndSession(params), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"User batch code: $FakeUserBatchCode not found."
        val _ = userBatchRepo.getUserBatchByCode(*[String]) wasCalled once
        val _ = userBatchRepo wasNever calledAgain
      }
    }

    "engagement is not found" should {
      "fail" in {
        val _      = userBatchRepo.getUserBatchByCode(*[String]) returns Future.value(Some(FakeUserBatch))
        val _      = engagementRepo.getEngagementByUserBatchCode(*[String]) returns Future.None
        val params = FakeCreateUserSessionParams

        val thrown = intercept[BizException] {
          Await.result(testInstance.createUserAndSession(params), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"Engagement doesn't exist for this user batch code: $FakeUserBatchCode"
        val _ = userBatchRepo.getUserBatchByCode(*[String]) wasCalled once
        val _ = engagementRepo.getEngagementByUserBatchCode(*[String]) wasCalled once
        val _ = userBatchRepo wasNever calledAgain
        val _ = engagementRepo wasNever calledAgain
      }
    }

    "user batch code is valid and engagment is found but user is already created" should {
      "throw BizException" in {
        val _ = userBatchRepo.getUserBatchByCode(*[String]) returns Future.value(Some(FakeUserBatch))
        val _ = engagementRepo.getEngagementByUserBatchCode(*[String]) returns Future.value(Some(FakeEngagement))
        val _ =
          userRepo.createUser(*[User]) returns Future.exception(
            BizException(s"Session id: $FakeSessionId is already created.")
          )
        val params = FakeCreateUserSessionParams

        val thrown = intercept[BizException] {
          Await.result(testInstance.createUserAndSession(params), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"Session id: $FakeSessionId is already created."
        val _ = userBatchRepo.getUserBatchByCode(*[String]) wasCalled once
        val _ = engagementRepo.getEngagementByUserBatchCode(*[String]) wasCalled once
        val _ = userRepo.createUser(*[User]) wasCalled once
        val _ = userBatchRepo wasNever calledAgain
        val _ = engagementRepo wasNever calledAgain
        val _ = userRoleRepo wasNever calledAgain
      }
    }

    "user batch code is valid and engagment is found" should {
      "create user, episode and test session" in {
        val _ = userBatchRepo.getUserBatchByCode(*[String]) returns Future.value(Some(FakeUserBatch))
        val _ = engagementRepo.getEngagementByUserBatchCode(*[String]) returns Future.value(Some(FakeEngagement))
        val _ = userRepo.createUser(*[User]) returns Future.value(FakeUser)
        val _ = userRoleRepo.createUserRole(*[UserRole]) returns Future.value(FakeUserRole)
        val _ = userAccountRepo.createUserAccount(*[UserAccount]) returns Future.value(FakeUserAccount)
        val _ = revInfoRepo.createRevInfo(*[RevInfo]) returns Future.value(FakeRevInfo)
        val _ = userAccountAudRepo.createUserAccountAud(*[UserAccountAud]) returns Future.value(FakeUserAccountAud)
        val _ =
          coreHttpClient.getCurrentTestSession(*[CurrentTestSession]) returns Future.value(
            FakeMagicLinkTestSessionDetail
          )
        val _ =
          episodeRepo.insertEpisodeAndTestSession(*[Episode], *[TestSession]) returns Future.value(
            (FakeNewEpisode, FakeTestSession)
          )
        val params = FakeCreateUserSessionParams

        val result = Await.result(testInstance.createUserAndSession(params), 1.second)

        val _ = result shouldBe dbfsConfig.magicLinkPath + "/" + FakeSessionId
        val _ = userBatchRepo.getUserBatchByCode(*[String]) wasCalled once
        val _ = engagementRepo.getEngagementByUserBatchCode(*[String]) wasCalled once
        val _ = userRepo.createUser(*[User]) wasCalled once
        val _ = userRoleRepo.createUserRole(*[UserRole]) wasCalled once
        val _ = userAccountRepo.createUserAccount(*[UserAccount]) wasCalled once
        val _ = revInfoRepo.createRevInfo(*[RevInfo]) wasCalled once
        val _ = userAccountAudRepo.createUserAccountAud(*[UserAccountAud]) wasCalled once
        val _ = coreHttpClient.getCurrentTestSession(*[CurrentTestSession]) wasCalled once
        val _ = episodeRepo.insertEpisodeAndTestSession(*[Episode], *[TestSession]) wasCalled once
        val _ = userBatchRepo wasNever calledAgain
        val _ = engagementRepo wasNever calledAgain
        val _ = userRoleRepo wasNever calledAgain
        val _ = userAccountRepo wasNever calledAgain
        val _ = revInfoRepo wasNever calledAgain
        val _ = userAccountAudRepo wasNever calledAgain
        val _ = userRepo wasNever calledAgain
        val _ = coreHttpClient wasNever calledAgain
        val _ = episodeRepo wasNever calledAgain
      }
    }

  }

  "updateSession" when {
    "user email is found" should {
      "throw UserExistedException" in {
        val params = UpdateSessionParams(FakeSessionId, FakeValidEmail)
        val _      = userRepo.getUserByUsername(*[String]) returns Future.value(Some(FakeUser))

        val thrown = intercept[UserExistsException] {
          Await.result(testInstance.updateSession(params), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"User with this email '$FakeValidEmail' already exist"
        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = emailPublisher wasNever called
      }
    }
    "session id is not found" should {
      "throw InvalidOtpException" in {
        val params = UpdateSessionParams(FakeSessionId, FakeValidEmail)
        val _      = userRepo.getUserByUsername(FakeValidEmail) returns Future.None
        val _      = userRepo.getUserByUsername(FakeSessionId) returns Future.None

        val thrown = intercept[UserNotFoundException] {
          Await.result(testInstance.updateSession(params), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"User with this session '$FakeSessionId' is missing"
        val _ = userRepo.getUserByUsername(*[String]) wasCalled twice
        val _ = userRepo wasNever calledAgain
        val _ = emailPublisher wasNever called
      }
    }

    "session id is found and email is not registered with any user" should {
      "send email" in {
        val emailCaptor = ArgCaptor[EmailOtpArgs]
        val params      = UpdateSessionParams(FakeSessionId, FakeValidEmail)
        val _           = userRepo.getUserByUsername(FakeValidEmail) returns Future.None
        val _           = userRepo.getUserByUsername(FakeSessionId) returns Future.value(Some(FakeUser))
        val _           = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(None)
        val _           = sessionOtpRepo.insertSessionOtp(*[SessionOtp]) returns Future.value(FakeSessionOtp)
        val _           = emailPublisher.publishOtpEmail(emailCaptor, *[Set[String]]) returns Future.Unit

        val _ = Await.result(testInstance.updateSession(params), 1.second)
        val _ = emailCaptor.value.otp shouldBe FakeSessionOtp.otpValue

        val _ = userRepo.getUserByUsername(*[String]) wasCalled twice
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = sessionOtpRepo.insertSessionOtp(*[SessionOtp]) wasCalled once
        val _ = emailPublisher.publishOtpEmail(*[EmailOtpArgs], *[Set[String]]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = emailPublisher wasNever calledAgain
      }
    }

    "session id is found and email is not registered with any user" should {
      "reset otp and send email" in {
        val emailCaptor = ArgCaptor[EmailOtpArgs]
        val params      = UpdateSessionParams(FakeSessionId, FakeValidEmail)
        val _           = userRepo.getUserByUsername(FakeValidEmail) returns Future.None
        val _           = userRepo.getUserByUsername(FakeSessionId) returns Future.value(Some(FakeUser))
        val _           = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))
        val _           = sessionOtpRepo.updateSessionOtp(*[SessionOtp]) returns Future.value(DefaultLongId)
        val _           = emailPublisher.publishOtpEmail(emailCaptor, *[Set[String]]) returns Future.Unit

        val _ = Await.result(testInstance.updateSession(params), 1.second)

        val _ = userRepo.getUserByUsername(*[String]) wasCalled twice
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = sessionOtpRepo.updateSessionOtp(*[SessionOtp]) wasCalled once
        val _ = emailPublisher.publishOtpEmail(*[EmailOtpArgs], *[Set[String]]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = emailPublisher wasNever calledAgain
      }
    }
  }

  "verifySession" when {
    "otp is expired" should {
      "throw OtpExpiredException" in {
        val params            = FakeSessionParams
        val _                 = userRepo.getUserByUsername(FakeValidEmail) returns Future.None
        val _                 = userRepo.getUserByUsername(FakeSessionId) returns Future.value(Some(FakeUser))
        val expiredSessionOtp = FakeSessionOtp.copy(utcExpiredAt = LocalDateTime.now().minusDays(1))
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(Some(expiredSessionOtp))

        val thrown = intercept[OtpExpiredException] {
          Await.result(testInstance.verifySession(params), 1.second)
        }

        val _ =
          thrown.getMessage shouldBe s"User with this session '$FakeSessionId', email '$FakeValidEmail', otp has expired."
        val _ = userRepo.getUserByUsername(*[String]) wasCalled twice
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = sessionOtpRepo wasNever calledAgain
      }
    }

    "otp attempts exceeded retries" should {
      "throw OtpTriesExceededException" in {
        val params                     = FakeSessionParams
        val _                          = userRepo.getUserByUsername(FakeValidEmail) returns Future.None
        val _                          = userRepo.getUserByUsername(FakeSessionId) returns Future.value(Some(FakeUser))
        val attemptsExceededSessionOtp = FakeSessionOtp.copy(attemptCount = 5)
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(Some(attemptsExceededSessionOtp))

        val thrown = intercept[OtpTriesExceededException] {
          Await.result(testInstance.verifySession(params), 1.second)
        }

        val _ =
          thrown.getMessage shouldBe s"User with this session '$FakeSessionId', email '$FakeValidEmail' has exceeded no of tries."
        val _ = userRepo.getUserByUsername(*[String]) wasCalled twice
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = sessionOtpRepo wasNever calledAgain
      }
    }

    "otp given is incorrect" should {
      "throw OtpMismatchException" in {
        val params = FakeSessionParams.copy(otp = FakeInvalidOtp)
        val _      = userRepo.getUserByUsername(FakeValidEmail) returns Future.None
        val _      = userRepo.getUserByUsername(FakeSessionId) returns Future.value(Some(FakeUser))
        val _      = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))
        val _      = sessionOtpRepo.updateSessionOtp(*[SessionOtp]) returns Future.value(DefaultLongId)

        val thrown = intercept[OtpMismatchException] {
          Await.result(testInstance.verifySession(params), 1.second)
        }

        val _ =
          thrown.getMessage shouldBe s"User with this session '$FakeSessionId', email '$FakeValidEmail' and otp '$FakeInvalidOtp' is incorrect."
        val _ = userRepo.getUserByUsername(*[String]) wasCalled twice
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = sessionOtpRepo.updateSessionOtp(*[SessionOtp]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = sessionOtpRepo wasNever calledAgain
      }
    }

    "session, email, expiry date, attempt count, otp value all passed" should {
      "return jwt that is valid" in {
        val params = FakeSessionParams
        val _      = userRepo.getUserByUsername(FakeValidEmail) returns Future.None
        val _      = userRepo.getUserByUsername(FakeSessionId) returns Future.value(Some(FakeUser))
        val _      = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))
        val _      = sessionOtpRepo.updateSessionOtp(*[SessionOtp]) returns Future.value(DefaultLongId)

        val result = Await.result(testInstance.verifySession(params), 1.second)

        val _ = JwtUtil.isTokenValid(result) shouldBe true
        val _ = userRepo.getUserByUsername(*[String]) wasCalled twice
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = sessionOtpRepo.updateSessionOtp(*[SessionOtp]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = sessionOtpRepo wasNever calledAgain
      }
    }

    "session and/or email is not found in session otp table" should {
      "return SessionNotFoundException" in {
        val params = FakeSessionParams.copy(otp = FakeInvalidOtp)
        val _      = userRepo.getUserByUsername(FakeValidEmail) returns Future.None
        val _      = userRepo.getUserByUsername(FakeSessionId) returns Future.value(Some(FakeUser))
        val _      = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(None)

        val thrown = intercept[SessionNotFoundException] {
          Await.result(testInstance.verifySession(params), 1.second)
        }

        val _ =
          thrown.getMessage shouldBe s"User with this session '$FakeSessionId' and email '$FakeValidEmail' not found."
        val _ = userRepo.getUserByUsername(*[String]) wasCalled twice
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = sessionOtpRepo wasNever calledAgain
      }
    }
  }

  "login" when {
    "existing user is found" should {
      "succeed and send email" in {
        val params = LoginParams(FakeSessionId, FakeValidEmail)
        val _      = userRepo.getUserByUsername(*[String]) returns Future.value(Some(FakeUser))
        val _      = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(None)
        val _      = sessionOtpRepo.insertSessionOtp(*[SessionOtp]) returns Future.value(FakeSessionOtp)

        val _ = Await.result(testInstance.login(params), 1.second)

        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = sessionOtpRepo.insertSessionOtp(*[SessionOtp]) wasCalled once
        val _ = userRepo wasNever calledAgain
        val _ = sessionOtpRepo wasNever calledAgain
      }
    }

    "existing user is not found" should {
      "throw UserNotFoundException" in {
        val params = LoginParams(FakeSessionId, FakeValidEmail)
        val _      = userRepo.getUserByUsername(*[String]) returns Future.value(None)

        val thrown = intercept[UserNotFoundException] {
          Await.result(testInstance.login(params), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"Username '$FakeValidEmail' is missing"
        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userRepo wasNever calledAgain
      }
    }
  }

  "verifyLogin" when {
    "otp is expired" should {
      "throw OtpExpiredException" in {
        val params            = VerifyLoginParams(FakeSessionId, FakeValidEmail, FakeValidOtp)
        val expiredSessionOtp = FakeSessionOtp.copy(utcExpiredAt = LocalDateTime.now().minusDays(1))
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(Some(expiredSessionOtp))

        val thrown = intercept[OtpExpiredException] {
          Await.result(testInstance.verifyLogin(params), 1.second)
        }

        val _ =
          thrown.getMessage shouldBe s"User with this session '$FakeSessionId', email '$FakeValidEmail', otp has expired."
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = sessionOtpRepo wasNever calledAgain
      }
    }

    "otp attempts exceeded retries" should {
      "throw OtpTriesExceededException" in {
        val params                     = VerifyLoginParams(FakeSessionId, FakeValidEmail, FakeValidOtp)
        val attemptsExceededSessionOtp = FakeSessionOtp.copy(attemptCount = 5)
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(Some(attemptsExceededSessionOtp))

        val thrown = intercept[OtpTriesExceededException] {
          Await.result(testInstance.verifyLogin(params), 1.second)
        }

        val _ =
          thrown.getMessage shouldBe s"User with this session '$FakeSessionId', email '$FakeValidEmail' has exceeded no of tries."
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = sessionOtpRepo wasNever calledAgain
      }
    }

    "otp given is incorrect" should {
      "throw OtpMismatchException" in {
        val params = VerifyLoginParams(FakeSessionId, FakeValidEmail, FakeInvalidOtp)
        val _      = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))
        val _      = sessionOtpRepo.updateSessionOtp(*[SessionOtp]) returns Future.value(DefaultLongId)

        val thrown = intercept[OtpMismatchException] {
          Await.result(testInstance.verifyLogin(params), 1.second)
        }

        val _ =
          thrown.getMessage shouldBe s"User with this session '$FakeSessionId', email '$FakeValidEmail' and otp '$FakeInvalidOtp' is incorrect."
        val _ = sessionOtpRepo.updateSessionOtp(*[SessionOtp]) wasCalled once
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = sessionOtpRepo wasNever calledAgain
      }
    }

    "session, email, expiry date, attempt count, otp value all passed" should {
      "return jwt that is valid if user found" in {
        val params = VerifyLoginParams(FakeSessionId, FakeValidEmail, FakeValidOtp)
        val _      = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))
        val _      = sessionOtpRepo.updateSessionOtp(*[SessionOtp]) returns Future.value(DefaultLongId)
        val _      = userRepo.getUserByUsername(*[String]) returns Future.value(Some(FakeUser))

        val result = Await.result(testInstance.verifyLogin(params), 1.second)

        val _ = JwtUtil.isTokenValid(result) shouldBe true
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = sessionOtpRepo.updateSessionOtp(*[SessionOtp]) wasCalled once
        val _ = sessionOtpRepo wasNever calledAgain
        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userRepo wasNever calledAgain
      }

      "return UserNotFoundException if user not found" in {
        val params = VerifyLoginParams(FakeSessionId, FakeValidEmail, FakeValidOtp)
        val _      = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))
        val _      = sessionOtpRepo.updateSessionOtp(*[SessionOtp]) returns Future.value(DefaultLongId)
        val _      = userRepo.getUserByUsername(*[String]) returns Future.None

        val thrown = intercept[UserNotFoundException] {
          Await.result(testInstance.verifyLogin(params), 1.second)
        }
        val _ = thrown.getMessage shouldBe s"Username '$FakeValidEmail' is missing"

        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = sessionOtpRepo.updateSessionOtp(*[SessionOtp]) wasCalled once
        val _ = sessionOtpRepo wasNever calledAgain
        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userRepo wasNever calledAgain
      }
    }

    "session and/or email is not found in session otp table" should {
      "return SessionNotFoundException" in {
        val params = VerifyLoginParams(FakeSessionId, FakeValidEmail, FakeInvalidOtp)
        val _      = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(None)

        val thrown = intercept[SessionNotFoundException] {
          Await.result(testInstance.verifyLogin(params), 1.second)
        }

        val _ =
          thrown.getMessage shouldBe s"User with this session '$FakeSessionId' and email '$FakeValidEmail' not found."
        val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
        val _ = sessionOtpRepo wasNever calledAgain
      }
    }
  }

  "sendUserReport" should {
    "return response as a string" in {
      val params = SendUserReport(FakeSessionId, FakeUserBatchCode)
      val _      = coreHttpClient.sendUserReport(*[SendUserReport]) returns Future.value(FakeSendUserReport)

      val result = Await.result(testInstance.sendUserReport(params), 1.second)

      val _ = result shouldBe FakeSendUserReport
      val _ = coreHttpClient.sendUserReport(*[SendUserReport]) wasCalled once
      val _ = coreHttpClient wasNever calledAgain
    }
  }

  "createDependencyDataForRepeatedUser" should {
    "don't create new revision and user account audit when user account aud has 2 entries" in {
      userAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) returns Future.value(Some(FakeUserAccount))
      userAccountAudRepo.getUserAccountAudById(*[Long]) returns Future.value(
        Seq(FakeUserAccountAud, FakeUserAccountAud)
      )

      val result: Unit = Await.result(testInstance.createDependencyDataForRepeatedUser(FakeUserIdAlpha, FakeUserBatchId),
                                      1.second
                                     )

      val _ = result shouldBe ()
      val _ = userAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
      val _ = userAccountAudRepo.getUserAccountAudById(*[Long]) wasCalled once
      val _ = userAccountRepo wasNever calledAgain
      val _ = userAccountAudRepo wasNever calledAgain
    }

    "when no user account aud is found" in {
      userAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) returns Future.value(Some(FakeUserAccount))
      userAccountAudRepo.getUserAccountAudById(*[Long]) returns Future.value(Seq())

      val thrown = intercept[BizException] {
        Await.result(testInstance.createDependencyDataForRepeatedUser(FakeUserIdAlpha, FakeUserBatchId), 1.second)
      }

      val _ =
        thrown.getMessage shouldBe s"User account aud is empty or more than 2 entries for repeated user " +
          s"with userId '${FakeUserIdAlpha.toString}' and userBatchId '${FakeUserBatchId.toString}'. "
      val _ = userAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
      val _ = userAccountAudRepo.getUserAccountAudById(*[Long]) wasCalled once
      val _ = userAccountRepo wasNever calledAgain
      val _ = userAccountAudRepo wasNever calledAgain
    }

    "when no user account is found" in {
      userAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) returns Future.value(None)

      val thrown = intercept[BizException] {
        Await.result(testInstance.createDependencyDataForRepeatedUser(FakeUserIdAlpha, FakeUserBatchId), 1.second)
      }

      val _ =
        thrown.getMessage shouldBe s"User account entry not found for userId '${FakeUserIdAlpha.toString}' and userBatchId '${FakeUserBatchId.toString}'. "
      val _ = userAccountRepo.getUserAccountByUserIdAndUserBatchId(*[Long], *[Long]) wasCalled once
      val _ = userAccountRepo wasNever calledAgain
    }
  }

  "createEpisodeAndTestSession" should {
    "handle errors when insertEpisodeAndTestSession fails" in {
      val _ = {
        coreHttpClient.getCurrentTestSession(*[CurrentTestSession]) returns Future.value(FakeMagicLinkTestSessionDetail)
        val _ =
          episodeRepo.insertEpisodeAndTestSession(*[Episode], *[TestSession]) returns Future.exception(
            BizException("error-message")
          )
      }

      val thrown = intercept[BizException] {
        Await.result(testInstance.createEpisodeAndTestSession(1L, 2L, 3L, "session123"), 1.second)
      }

      val _ = thrown.getMessage shouldBe "Failed to create test session for sessionId session123."
      val _ = coreHttpClient.getCurrentTestSession(*[CurrentTestSession]) wasCalled once
      val _ = coreHttpClient wasNever calledAgain
    }

    "handle errors when fetching current test session fails" in {
      val _ =
        coreHttpClient.getCurrentTestSession(*[CurrentTestSession]) returns Future.exception(
          BizException("error-message")
        )

      val thrown = intercept[BizException] {
        Await.result(testInstance.createEpisodeAndTestSession(1L, 2L, 3L, "session123"), 1.second)
      }

      val _ = thrown.getMessage shouldBe "error-message"
      val _ = coreHttpClient.getCurrentTestSession(*[CurrentTestSession]) wasCalled once
      val _ = coreHttpClient wasNever calledAgain
    }
  }

  "createUserSurvey" when {
    "user exists" should {
      "create survey" in {
        val _      = userRepo.getUserByUsername(*[String]) returns Future.value(Some(FakeUser))
        val _      = userSurveyRepo.createUserSurvey(*[UserSurvey]) returns Future.value(FakeSessionId)
        val params = CreateUserSurveyParams(FakeSessionId, FakeUserName, List(FakeSurveyItem))

        val _ = Await.result(testInstance.createUserSurvey(params), 1.second)

        val _ = userSurveyRepo.createUserSurvey(*[UserSurvey]) wasCalled once
        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userSurveyRepo wasNever calledAgain
        val _ = userRepo wasNever calledAgain
      }
    }
    "user does not exist" should {
      "not create survey" in {
        val _ = userRepo.getUserByUsername(*) returns Future.None

        val params = CreateUserSurveyParams(FakeSessionId, FakeUserName, List(FakeSurveyItem))
        val thrown = intercept[UserNotFoundException] {
          Await.result(testInstance.createUserSurvey(params), 1.second)
        }

        val _ = thrown.getMessage shouldBe s"User '$FakeSessionId' does not exist"

        val _ = userRepo.getUserByUsername(*[String]) wasCalled once
        val _ = userSurveyRepo wasNever called
        val _ = userRepo wasNever calledAgain
      }
    }
  }

  "isNewSessionAllowedByUserName" when {
    "no latest episode found" should {
      "allow new session" in {
        val _ = episodeRepo.getLatestEpisodeByUsername(*[String]) returns Future.None

        val res = Await.result(testInstance.isNewSessionAllowedByUserName(CheckNewSessionAllowedParams(FakeUserName)),
                               1.second
                              )
        res shouldBe true

        val _ = episodeRepo.getLatestEpisodeByUsername(*[String]) wasCalled once
        val _ = episodeRepo wasNever calledAgain
      }
    }
    "latest episode found" should {
      "return true if not same day as today" in {
        val _ =
          episodeRepo.getLatestEpisodeByUsername(*[String]) returns
            Future.value(Some(FakeEpisodeAlpha.copy(maybeUtcStartAt = Some(FakeLocalDateTimeNow.minusDays(1)))))

        val res = Await.result(testInstance.isNewSessionAllowedByUserName(CheckNewSessionAllowedParams(FakeUserName)),
                               1.second
                              )
        res shouldBe true

        val _ = episodeRepo.getLatestEpisodeByUsername(*[String]) wasCalled once
        val _ = episodeRepo wasNever calledAgain
      }
      "return false if same day as today" in {
        val _ = episodeRepo.getLatestEpisodeByUsername(*[String]) returns Future.value(Some(FakeEpisodeAlpha))

        val res = Await.result(testInstance.isNewSessionAllowedByUserName(CheckNewSessionAllowedParams(FakeUserName)),
                               1.second
                              )
        res shouldBe false

        val _ = episodeRepo.getLatestEpisodeByUsername(*[String]) wasCalled once
        val _ = episodeRepo wasNever calledAgain
      }
    }
  }

  "getSessionOtp" should {
    "call sessionOtpRepo.getSessionOtp" in {
      val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) returns Future.value(Some(FakeSessionOtp))

      val res = Await.result(testInstance.getSessionOtp(FakeSessionId, FakeValidEmail), 1.second)

      val _ = res shouldBe Some(FakeSessionOtp)
      val _ = sessionOtpRepo.getSessionOtp(*[String], *[String]) wasCalled once
      val _ = sessionOtpRepo wasNever calledAgain
    }
  }

  "SessionService.invalidateSessionOtp" should {
    "succeed" in {
      sessionOtpRepo.invalidateSessionOtp(*[String], *[String]) returns Future.Unit

      val res: Unit = Await.result(testInstance.invalidateSessionOtp(FakeSessionId, FakeValidEmail), 1.second)

      val _ = res shouldBe ()
      val _ = sessionOtpRepo.invalidateSessionOtp(*[String], *[String]) wasCalled once
      val _ = sessionOtpRepo wasNever calledAgain
    }
  }

  "getLatestCompletedSession" should {
    "return a LatestTestSession with isScoreReady true if the test session has a Z-Score" in {
      val TsWithZScore = FakeTestSession.copy(maybeZScore = Some("some-z-scores"))
      val _ =
        episodeRepo.getLatestCompletedTestSessionsByUsername(*[String]) returns Future.value(
          Some((FakeEpisodeAlpha, TsWithZScore))
        )

      val result = Await.result(testInstance.getLatestCompletedSession(LatestCompletedSessionParams(FakeValidEmail)),
                                1.second
                               )

      val _ = result.sessionId shouldBe FakeEpisodeRefAlpha
      val _ = result.isScoreReady shouldBe true
      val _ = episodeRepo.getLatestCompletedTestSessionsByUsername(*[String]) wasCalled once
      val _ = episodeRepo wasNever calledAgain
    }

    "return a LatestTestSession with isScoreReady false if the test session does not have a Z-Score" in {
      val _ =
        episodeRepo.getLatestCompletedTestSessionsByUsername(*[String]) returns Future.value(
          Some((FakeEpisodeAlpha, FakeTestSession))
        )

      val result = Await.result(testInstance.getLatestCompletedSession(LatestCompletedSessionParams(FakeValidEmail)),
                                1.second
                               )

      val _ = result.sessionId shouldBe FakeEpisodeRefAlpha
      val _ = result.isScoreReady shouldBe false
      val _ = episodeRepo.getLatestCompletedTestSessionsByUsername(*[String]) wasCalled once
      val _ = episodeRepo wasNever calledAgain
    }

    "throw a SessionNotFoundException if no session is found" in {
      val _ = episodeRepo.getLatestCompletedTestSessionsByUsername(*[String]) returns Future.None

      val thrown = intercept[SessionNotFoundException] {
        Await.result(testInstance.getLatestCompletedSession(LatestCompletedSessionParams(FakeValidEmail)), 1.second)
      }

      val _ = thrown.getMessage shouldBe s"Test session not found for email: $FakeValidEmail"
      val _ = episodeRepo.getLatestCompletedTestSessionsByUsername(*[String]) wasCalled once
      val _ = episodeRepo wasNever calledAgain
    }
  }

  "getEndOfDayLocalDateTime" should {
    "getEndOfDayLocalDateTime should should return 18:29:59 for India if server time is UTC" in {
      val zoneIdString = "Asia/Kolkata"
      val zoneId       = ZoneId.of(zoneIdString)

      val expectedDate     = LocalDate.now(zoneId)
      val expectedTime     = LocalTime.of(18, 29, 59, 999999999)
      val expectedDateTime = LocalDateTime.of(expectedDate, expectedTime)

      val result = SessionServiceImpl.getUtcLocalEndOfDay(zoneIdString)

      result shouldBe expectedDateTime
    }

    "getEndOfDayLocalDateTime should return 15:59:59 for Singapore if server time is UTC" in {
      val zoneIdString = "Asia/Singapore"
      val zoneId       = ZoneId.of(zoneIdString)

      val expectedDate     = LocalDate.now(zoneId)
      val expectedTime     = LocalTime.of(15, 59, 59, 999999999)
      val expectedDateTime = LocalDateTime.of(expectedDate, expectedTime)

      val result = SessionServiceImpl.getUtcLocalEndOfDay(zoneIdString)

      result shouldBe expectedDateTime
    }
  }

}

object SessionServiceImplTest {

  val FakePayload = EnqueueTestSessionParams(
    FakeRequestId,
    FakeUserIdAlpha.toString,
    FakeEpisodeRefAlpha,
    FakeUserIdAlpha.toString,
    FakeEpisodeRefAlpha,
    FakeLocationId,
    FakeFirstName,
    FakeLastName,
    FakeDob,
    FakeGender,
    None,
    None
  )

}

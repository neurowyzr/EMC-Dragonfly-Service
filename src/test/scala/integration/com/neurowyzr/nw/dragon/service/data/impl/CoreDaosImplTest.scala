package com.neurowyzr.nw.dragon.service.data.impl

import com.twitter.util.{Return, Throw, Try}

import com.neurowyzr.nw.dragon.service.SharedFakes.*
import com.neurowyzr.nw.dragon.service.cfg.Models.DatabaseConfig
import com.neurowyzr.nw.dragon.service.data.CoreSqlDbContext
import com.neurowyzr.nw.dragon.service.data.impl.CoreDaosImplTest.{InitialisationTimeInMs, TestDao}
import com.neurowyzr.nw.dragon.service.data.impl.Fakes.*
import com.neurowyzr.nw.dragon.service.data.models.*

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.SnakeCase
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class CoreDaosImplTest extends AnyFunSuite with Matchers with OptionValues with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    Thread.sleep(InitialisationTimeInMs)
    TestDao.deleteAllEpisodes()
    TestDao.deleteAllTestSessions()
    TestDao.deleteAllUserAccounts()
    TestDao.deleteAllUsers()
    TestDao.deleteAllUserBatches()
    TestDao.deleteAllEngagements()
    TestDao.deleteAllClients()
    TestDao.deleteAllProducts()
    TestDao.deleteAllSessionOtp()

    val tryClientId  = TestDao.insertNewClient(FakeClient)
    val tryProductId = TestDao.insertNewProduct(FakeProduct)
    val _ = TestDao.insertNewEngagement(
      FakeEngagement.copy(clientId = tryClientId.get(), productId = tryProductId.get())
    )
  }

  test("insert episode succeeds") {
    val currentAllEpisodes = TestDao.allEpisodes()
    val _                  = currentAllEpisodes.get().size shouldBe 0

    val engagementId = getOnlyEngagementId
    val tryUserBatchId = TestDao.insertNewUserBatch(
      FakeUserBatch.copy(engagementId = engagementId, maybeCode = Some(FakeUserBatchCode))
    )
    val tryUserId   = TestDao.insertNewUser(FakeUser)
    val fakeEpisode = FakeNewEpisode.copy(userId = tryUserId.get())
    val fakeTestSession = FakeTestSession.copy(
      userId = tryUserId.get(),
      userBatchId = tryUserBatchId.get(),
      engagementId = engagementId,
      maybeUtcCompletedAt = Some(FakeLocalDateTimeNowAlpha),
      maybeZScore = Some("{\"some-scores\":\"scores\"}")
    )
    val tryMaybeEpisodeAndTestSession = TestDao.insertEpisodeAndTestSession(fakeEpisode, fakeTestSession)

    val _                   = tryMaybeEpisodeAndTestSession shouldBe a[Return[?]]
    val returnedEpisode     = tryMaybeEpisodeAndTestSession.get()._1
    val returnedTestSession = tryMaybeEpisodeAndTestSession.get()._2
    val _                   = returnedTestSession shouldBe fakeTestSession.copy(id = returnedTestSession.id)
    val _ = returnedEpisode shouldBe fakeEpisode.copy(id = returnedEpisode.id, testSessionId = returnedTestSession.id)

    val allEpisodes     = TestDao.allEpisodes()
    val _               = allEpisodes.get().size shouldBe 1
    val expectedEpisode = allEpisodes.get().head
    val _               = expectedEpisode.maybeUtcCreatedAt should not be empty
    val _ = allEpisodes.get().head shouldBe returnedEpisode.copy(maybeUtcCreatedAt = expectedEpisode.maybeUtcCreatedAt)

    val tryMaybeEpisodeTestSession = TestDao.getLatestCompletedTestSessionsByUsername(FakeUserName)

    val _ = tryMaybeEpisodeTestSession shouldBe a[Return[?]]
    val _ = tryMaybeEpisodeTestSession.get().isDefined shouldBe true
    //    val _ = tryMaybeEpisodeTestSession.get().get.maybeMessageId.get shouldBe FakeMessageId

  }

  test("insert episode failed due to foreign key constraint") {
    val tryMaybeUser       = TestDao.getUserByExtPatientRef(FakeExternalPatientRef)
    val tryMaybeUserBatch  = TestDao.getUserBatchByCode(FakeUserBatchCode)
    val tryMaybeEngagement = TestDao.getEngagementByUserBatchCode(FakeUserBatchCode)
    val fakeValidEpisode   = FakeNewEpisode.copy(userId = tryMaybeUser.get().value.id)
    val fakeValidTestSession = FakeTestSession.copy(userId = tryMaybeUser.get().value.id,
                                                    userBatchId = tryMaybeUserBatch.get().get.id,
                                                    engagementId = tryMaybeEngagement.get().value.id
                                                   )
    val fakeInvalidTestSession = fakeValidTestSession.copy(userId = 9999999L)

    val tryMaybeEpisodeAndTestSession = TestDao.insertEpisodeAndTestSession(fakeValidEpisode, fakeInvalidTestSession)

    val _ = tryMaybeEpisodeAndTestSession shouldBe a[Throw[?]]
    val _ = tryMaybeEpisodeAndTestSession.isThrow shouldEqual true

    tryMaybeEpisodeAndTestSession.throwable.getMessage.toLowerCase shouldBe
      "Cannot add or update a child row: a foreign key constraint fails (`cognifyx-core-test`.`TEST_SESSION`, CONSTRAINT `as_fk_user_id_idx` FOREIGN KEY (`user_id`) REFERENCES `USERS` (`id`) ON DELETE CASCADE)".toLowerCase
  }

  test("insert episode failed and test session is not committed to database as both inserts are part of transaction") {
    // Before the test, there should be only one test session in the database
    val currentAllTestSessions = TestDao.allTestSessions()
    val _                      = currentAllTestSessions.get().size shouldBe 1

    val tryMaybeUser       = TestDao.getUserByExtPatientRef(FakeExternalPatientRef)
    val tryMaybeUserBatch  = TestDao.getUserBatchByCode(FakeUserBatchCode)
    val tryMaybeEngagement = TestDao.getEngagementByUserBatchCode(FakeUserBatchCode)
    val fakeValidEpisode   = FakeNewEpisode.copy(maybeMessageId = Some("some-random-message-id"))
    val fakeValidTestSession = FakeTestSession.copy(userId = tryMaybeUser.get().value.id,
                                                    userBatchId = tryMaybeUserBatch.get().get.id,
                                                    engagementId = tryMaybeEngagement.get().value.id
                                                   )
    val fakeInvalidEpisode = fakeValidEpisode.copy(userId = 9999999L)

    val tryMaybeEpisodeAndTestSession = TestDao.insertEpisodeAndTestSession(fakeInvalidEpisode, fakeValidTestSession)

    val _ = tryMaybeEpisodeAndTestSession shouldBe a[Throw[?]]
    val _ = tryMaybeEpisodeAndTestSession.isThrow shouldEqual true
    val _ =
      tryMaybeEpisodeAndTestSession.throwable.getMessage.toLowerCase shouldBe
        "Cannot add or update a child row: a foreign key constraint fails (`cognifyx-core-test`.`EPISODE`, CONSTRAINT `ul_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `USERS` (`id`))".toLowerCase

    // After the test, there should still be only one test session in the database
    val updatedAllTestSessions = TestDao.allTestSessions()
    val _                      = updatedAllTestSessions.get().size shouldBe 1
  }

  test("get episode by test id succeeds") {
    checkEpisodeTableHasSingleEntry()

    val tryMaybeEpisode = TestDao.getEpisodeByTestId(FakeNewEpisodeRef)

    val _ = tryMaybeEpisode shouldBe a[Return[?]]
    val _ = tryMaybeEpisode.get().isDefined shouldBe true
    val _ = tryMaybeEpisode.get().get.episodeRef shouldBe FakeNewEpisodeRef
  }

  test("get episode by message id succeeds") {
    checkEpisodeTableHasSingleEntry()

    val tryMaybeEpisode = TestDao.getEpisodeByMessageId(FakeMessageId)

    val _ = tryMaybeEpisode shouldBe a[Return[?]]
    val _ = tryMaybeEpisode.get().isDefined shouldBe true
    val _ = tryMaybeEpisode.get().get.maybeMessageId.get shouldBe FakeMessageId
  }

  test("get episode by username succeeds") {
    val tryMaybeEpisode = TestDao.getLatestEpisodeByUsername(FakeUserName)

    val _ = tryMaybeEpisode shouldBe a[Return[?]]
    val _ = tryMaybeEpisode.get().isDefined shouldBe true
    val _ = tryMaybeEpisode.get().get.maybeMessageId.get shouldBe FakeMessageId
  }

  test("update episode succeeds") {
    val tryMaybeEpisode = TestDao.getEpisodeByMessageId(FakeMessageId)
    val episodeId       = tryMaybeEpisode.get().get.id
    val _               = tryMaybeEpisode.get().get.maybeUtcUpdatedAt shouldBe empty

    val tryEpisode = TestDao.updateEpisodeExpiryDate(episodeId, FakeLocalDateTimeNowBravo.plusDays(123))

    val maybeEpisode = TestDao.getEpisodeById(episodeId)
    val _            = tryEpisode shouldBe a[Return[?]]
    val _            = maybeEpisode shouldBe a[Return[?]]
    val _            = maybeEpisode.get().isDefined shouldBe true
    val _            = maybeEpisode.get().get.maybeUtcExpiryAt shouldBe Some(FakeLocalDateTimeNowBravo.plusDays(123))
    val _            = maybeEpisode.get().get.maybeUtcUpdatedAt should not be empty
  }

  test("invalidate episode succeeds") {
    val tryMaybeEpisode = TestDao.getEpisodeByMessageId(FakeMessageId)
    val episodeId       = tryMaybeEpisode.get().get.id
    val _               = tryMaybeEpisode.get().get.isInvalidated shouldBe false

    val tryEpisode = TestDao.invalidateEpisode(episodeId)

    val maybeEpisode = TestDao.getEpisodeById(episodeId)
    val _            = tryEpisode shouldBe a[Return[?]]
    val _            = maybeEpisode shouldBe a[Return[?]]
    val _            = maybeEpisode.get().isDefined shouldBe true
    val _            = maybeEpisode.get().get.isInvalidated shouldBe true
  }

  test("get episode by message id and source succeeds") {
    checkEpisodeTableHasSingleEntry()

    val tryMaybeEpisode = TestDao.getEpisodeByMessageIdAndSource(FakeMessageId, FakeSource)

    val _ = tryMaybeEpisode shouldBe a[Return[?]]
    val _ = tryMaybeEpisode.get().isDefined shouldBe true
    val _ = tryMaybeEpisode.get().get.maybeMessageId.get shouldBe FakeMessageId
    val _ = tryMaybeEpisode.get().get.maybeSource.get shouldBe FakeSource
  }

  test("get episode by episode ref and source succeeds") {
    checkEpisodeTableHasSingleEntry()

    val tryMaybeEpisode = TestDao.getEpisodeByEpisodeRefAndSource(FakeNewEpisodeRef, FakeSource)

    val _ = tryMaybeEpisode shouldBe a[Return[?]]
    val _ = tryMaybeEpisode.get().isDefined shouldBe true
    val _ = tryMaybeEpisode.get().get.episodeRef shouldBe FakeNewEpisodeRef
    val _ = tryMaybeEpisode.get().get.maybeSource.get shouldBe FakeSource
  }

  test("get test sessions by username and user batch code succeeds") {
    val currentAllTestSessions = TestDao.allTestSessions()
    val _                      = currentAllTestSessions.get().size shouldBe 1
    val currentTestSession     = currentAllTestSessions.get().head
    val userId                 = currentTestSession.userId
    val userBatchId            = currentTestSession.userBatchId

    val retrievedTestSessions = TestDao.getTestSessionsByUsernameAndUserBatch(FakeUserName, FakeUserBatchCode)

    val _                = retrievedTestSessions.get().size shouldBe 1
    val foundTestSession = retrievedTestSessions.get().head._1

    val _ = foundTestSession.userId shouldBe userId
    val _ = foundTestSession.userBatchId shouldBe userBatchId
  }

  test("insert new test session succeeds") {
    TestDao.deleteAllEpisodes()
    TestDao.deleteAllTestSessions()
    TestDao.deleteAllUsers()
    TestDao.deleteAllUserBatches()

    val currentAllTestSessions = TestDao.allTestSessions()
    val _                      = currentAllTestSessions.get().size shouldBe 0

    val engagementId = getOnlyEngagementId
    val tryUserBatchId = TestDao.insertNewUserBatch(
      FakeUserBatch.copy(engagementId = engagementId, maybeCode = Some(FakeUserBatchCode))
    )
    val tryUserId = TestDao.insertNewUser(FakeUser)
    val fakeTestSession = FakeTestSession.copy(userId = tryUserId.get(),
                                               userBatchId = tryUserBatchId.get(),
                                               engagementId = engagementId
                                              )
    val tryMaybeTestSessionId = TestDao.insertNewTestSession(fakeTestSession)

    val allTestSessions = TestDao.allTestSessions()
    val _               = allTestSessions.get().size shouldBe 1

    val _           = tryMaybeTestSessionId shouldBe a[Return[?]]
    val testSession = fakeTestSession.copy(id = tryMaybeTestSessionId.get())
    val _           = allTestSessions shouldBe Return(Seq(testSession))
  }

  test("insert new test session fails due to missing user") {
    TestDao.deleteAllEpisodes()
    TestDao.deleteAllTestSessions()
    TestDao.deleteAllUsers()

    val currentAllTestSessions = TestDao.allTestSessions()
    val _                      = currentAllTestSessions.get().size shouldBe 0

    val engagementId = getOnlyEngagementId
    val tryUserId    = TestDao.insertNewUser(FakeUser)
    val fakeTestSession = FakeTestSession.copy(userId = tryUserId.get(),
                                               userBatchId = 99999999L,
                                               engagementId = engagementId
                                              )

    val tryMaybeTestSessionId = TestDao.insertNewTestSession(fakeTestSession)

    val _ = tryMaybeTestSessionId shouldBe a[Throw[?]]
    val _ = tryMaybeTestSessionId.isThrow shouldEqual true
    val _ =
      tryMaybeTestSessionId.throwable.getMessage.toLowerCase shouldBe
        "Cannot add or update a child row: a foreign key constraint fails (`cognifyx-core-test`.`test_session`, CONSTRAINT `as_fk_user_batch_id_idx` FOREIGN KEY (`user_batch_id`) REFERENCES `user_batch` (`id`) ON DELETE CASCADE)".toLowerCase
  }

  test("update user succeeds") {
    TestDao.deleteAllUsers()

    val tryUserId        = TestDao.insertNewUser(FakeUser)
    val allUsers         = TestDao.allUsers()
    val _                = allUsers.get().size shouldBe 1
    val savedUserAccount = allUsers.get().head
    val _                = savedUserAccount shouldBe FakeUser.copy(id = tryUserId.get())

    val changedUserAccount = savedUserAccount.copy(username = "<USERNAME>", maybeEmailHash = Some("<EMAIL>"))

    val _           = TestDao.updateUser(changedUserAccount)
    val newAllUsers = TestDao.allUsers()
    val _           = newAllUsers.get().size shouldBe 1
    val _           = newAllUsers.get().head shouldBe changedUserAccount
  }

  test("delete user succeeds") {
    val _        = TestDao.deleteUserByUsername("<USERNAME>")
    val allUsers = TestDao.allUsers()
    val _        = allUsers.get().size shouldBe 1
    val _        = allUsers.get().head.username == "<USERNAME>" shouldBe false
  }

  test("delete user succeeds 2") {
    val _        = TestDao.deleteUserByUsername("<USERNAME>")
    val allUsers = TestDao.allUsers()
    val _        = allUsers.get().size shouldBe 1
    val _        = allUsers.get().head.username == "<USERNAME>" shouldBe false
  }

  test("insert new user account succeeds") {
    TestDao.deleteAllUserAccountsAud()
    TestDao.deleteAllRevInfo()
    TestDao.deleteAllUserAccounts()
    TestDao.deleteAllUserBatches()
    TestDao.deleteAllUserRoles()
    TestDao.deleteAllUsers()

    val engagementId     = getOnlyEngagementId
    val tryUserId        = TestDao.insertNewUser(FakeUser)
    val inputUserRole    = UserRole(tryUserId.get(), FakeRoleId)
    val returnedUserRole = TestDao.insertNewUserRole(inputUserRole)
    val tryUserBatchId = TestDao.insertNewUserBatch(
      FakeUserBatch.copy(engagementId = engagementId, maybeCode = Some(FakeUserBatchCode))
    )
    val fakeUserAccount  = UserAccount(tryUserId.get(), tryUserBatchId.get())
    val tryUserAccountId = TestDao.insertNewUserAccount(fakeUserAccount)

    val tryRevInfoId = TestDao.insertNewRevInfo(FakeRevInfo)

    val fakeUserAccountAud = UserAccountAud(tryUserAccountId.get(),
                                            tryRevInfoId.get(),
                                            Some(2),
                                            Some(FakeUserAccountConfig)
                                           )
    val tryUserAccountAudId = TestDao.insertNewUserAccountAud(fakeUserAccountAud)

    val revInfoSeq        = TestDao.getRevInfoById(tryRevInfoId.get())
    val userAccountAudSeq = TestDao.getUserAccountAudById(tryUserAccountId.get())

    val allUserAccounts = TestDao.allUserAccounts()
    val _               = allUserAccounts.size shouldBe 1

    val _           = tryUserAccountId shouldBe a[Return[?]]
    val _           = tryUserAccountAudId shouldBe a[Return[?]]
    val _           = tryRevInfoId shouldBe a[Return[?]]
    val _           = revInfoSeq.get().size shouldBe 1
    val _           = revInfoSeq.get().head shouldBe FakeRevInfo.copy(id = tryRevInfoId.get())
    val _           = userAccountAudSeq.get().size shouldBe 1
    val _           = userAccountAudSeq.get().head shouldBe fakeUserAccountAud
    val userAccount = fakeUserAccount.copy(id = tryUserAccountId.get())
    val _           = allUserAccounts shouldBe Seq(userAccount)

    val tryMaybeUserBatch = TestDao.getUserAccountByUserIdAndUserBatchId(tryUserId.get(), tryUserBatchId.get())

    val _ = tryMaybeUserBatch shouldBe a[Return[?]]
    val _ = tryMaybeUserBatch.get().isDefined shouldBe true
    val _ = tryMaybeUserBatch.get().value.id shouldBe tryUserAccountId.get()
  }

  test("insert new cygnus event succeeds") {
    TestDao.deleteAllCygnusEvents()

    val fakeCygnusEvent = CygnusEvent(FakeMessageType, FakeMessageId)

    val tryCygnusEvent = TestDao.insertNewCygnusEvent(fakeCygnusEvent)

    val tryMaybeCygnusEvent = TestDao.getCygnusEventByMessageTypeAndMessageId(FakeMessageType, FakeMessageId)
    val _                   = tryMaybeCygnusEvent.get().size shouldBe 1
    val expectedCygnusEvent = tryMaybeCygnusEvent.get().get

    val _ = tryCygnusEvent shouldBe a[Return[?]]
    val _ = expectedCygnusEvent.id shouldBe tryCygnusEvent.get()
    val _ = expectedCygnusEvent.messageId shouldBe fakeCygnusEvent.messageId
    val _ = expectedCygnusEvent.messageType shouldBe fakeCygnusEvent.messageType
    val _ = expectedCygnusEvent.maybeUtcCreatedAt should not be empty
  }

  test("insert new user account fails as the user and user batch already exist") {
    TestDao.deleteAllUserAccounts()
    TestDao.deleteAllUserBatches()
    TestDao.deleteAllUsers()

    val engagementId = getOnlyEngagementId
    val tryUserId    = TestDao.insertNewUser(FakeUser)
    val tryUserBatchId = TestDao.insertNewUserBatch(
      FakeUserBatch.copy(engagementId = engagementId, maybeCode = Some(FakeUserBatchCode))
    )
    val fakeUserAccount = UserAccount(tryUserId.get(), tryUserBatchId.get())

    val _                     = TestDao.insertNewUserAccount(fakeUserAccount)
    val tryMaybeUserAccountId = TestDao.insertNewUserAccount(fakeUserAccount)

    val allUserAccounts = TestDao.allUserAccounts()
    val _               = allUserAccounts.size shouldBe 1

    val _ = tryMaybeUserAccountId shouldBe a[Throw[?]]
    val _ = tryMaybeUserAccountId.isThrow shouldEqual true
    val _ = tryMaybeUserAccountId.throwable.getClass.getSimpleName shouldBe "SQLIntegrityConstraintViolationException"
  }

  test("get user by patient id succeeds") {
    val tryMaybeUser = TestDao.getUserByExtPatientRef(FakeExternalPatientRef)

    val _ = tryMaybeUser shouldBe a[Return[?]]
    val _ = tryMaybeUser.get().isDefined shouldBe true
    val _ = tryMaybeUser.get().value.maybeExternalPatientRef.value shouldBe FakeExternalPatientRef
  }

  test("get user by source and patient ref succeeds") {
    val tryMaybeUser = TestDao.getUserBySourceAndExtPatientRef(FakeSource, FakeExternalPatientRef)

    val _ = tryMaybeUser shouldBe a[Return[?]]
    val _ = tryMaybeUser.get().isDefined shouldBe true
    val _ = tryMaybeUser.get().value.maybeExternalPatientRef.value shouldBe FakeExternalPatientRef
    val _ = tryMaybeUser.get().value.maybeSource.value shouldBe FakeSource
  }

  test("get user by username succeeds") {
    val tryMaybeUser = TestDao.getUserByUsername(FakeUserName)

    val _ = tryMaybeUser shouldBe a[Return[?]]
    val _ = tryMaybeUser.get().isDefined shouldBe true
    val _ = tryMaybeUser.get().value.username shouldBe FakeUserName
  }

  test("get user by id succeeds") {
    val tryMaybeUser          = TestDao.getUserByUsername(FakeUserName)
    val tryMaybeUserDuplicate = TestDao.getUserById(tryMaybeUser.get().value.id)

    val _ = tryMaybeUser shouldBe tryMaybeUserDuplicate
  }

  test("get engagement by user batch code succeeds") {
    val tryMaybeEngagement = TestDao.getEngagementByUserBatchCode(FakeUserBatchCode)

    val _ = tryMaybeEngagement shouldBe a[Return[?]]
    val _ = tryMaybeEngagement.get().isDefined shouldBe true
    val _ = tryMaybeEngagement.get().value.id shouldBe getIdFromEngagementTableWithSingleEntry
  }

  test("get user batch by user batch code succeeds") {
    val tryMaybeUserBatch = TestDao.getUserBatchByCode(FakeUserBatchCode)

    val _ = tryMaybeUserBatch shouldBe a[Return[?]]
    val _ = tryMaybeUserBatch.get().isDefined shouldBe true
    val _ = tryMaybeUserBatch.get().value.maybeCode.value shouldBe FakeUserBatchCode
  }

  test("insert session otp succeeds") {
    val currentAllSessionOtps = TestDao.allSessionOtps()
    val _                     = currentAllSessionOtps.get().size shouldBe 0

    val trySessionOtpId = TestDao.insertSessionOtp(FakeSessionOtp)
    val _               = trySessionOtpId shouldBe a[Return[?]]

    val newAllSessionOtps = TestDao.allSessionOtps()
    val _                 = newAllSessionOtps.get().size shouldBe 1

    val maybeSessionDao = TestDao.getSessionOtp(FakeSessionOtp.sessionId, FakeSessionOtp.emailHash)
    val _               = maybeSessionDao shouldBe a[Return[?]]
    val _               = maybeSessionDao.get().isDefined shouldBe true
    val _               = maybeSessionDao.get().value.emailHash shouldBe FakeSessionOtp.emailHash
    val _               = maybeSessionDao.get().value.sessionId shouldBe FakeSessionOtp.sessionId
  }

  test("update session otp succeeds") {
    val currentAllSessionOtps = TestDao.allSessionOtps()
    val _                     = currentAllSessionOtps.get().size shouldBe 1
    val sessionOtpId          = currentAllSessionOtps.get().head.id

    val trySessionOtpId = TestDao.updateSessionOtp(
      FakeSessionOtp.copy(id = sessionOtpId, emailHash = "new@email.com", otpValue = "newotp", attemptCount = 1)
    )
    val _ = trySessionOtpId shouldBe a[Return[?]]

    val newAllSessionOtps = TestDao.allSessionOtps()
    val _                 = newAllSessionOtps.get().size shouldBe 1

    val maybeSessionDao = TestDao.getSessionOtp(FakeSessionId, "new@email.com")
    val _               = maybeSessionDao shouldBe a[Return[?]]
    val _               = maybeSessionDao.get().isDefined shouldBe true
    val _               = maybeSessionDao.get().value.emailHash shouldBe "new@email.com"
    val _               = maybeSessionDao.get().value.otpValue shouldBe "newotp"
    val _               = maybeSessionDao.get().value.attemptCount shouldBe 1
  }

  test("invalidate session otp succeeds") {
    val currentAllSessionOtps = TestDao.allSessionOtps()
    val _                     = currentAllSessionOtps.get().size shouldBe 1
    val sessionId             = currentAllSessionOtps.get().head.sessionId
    val emailHash             = currentAllSessionOtps.get().head.emailHash

    val trySessionOtpId = TestDao.invalidateSessionOtp(sessionId, emailHash)
    val _               = trySessionOtpId shouldBe a[Return[?]]

    val maybeSessionDao = TestDao.getSessionOtp(sessionId, emailHash)
    val _               = maybeSessionDao shouldBe a[Return[?]]
    val _               = maybeSessionDao.get().isDefined shouldBe true
    val _               = maybeSessionDao.get().value.maybeUtcInvalidatedAt shouldBe defined
  }

  test("logInsert should log successful creation") {
    val record = FakeNewEpisode

    val result = CoreDaosImpl.logInsert[Episode](Try(record), "episode", record => record.id.toString)

    val _ = result shouldBe a[Return[?]]
    val _ = result.get() shouldBe record
  }

  test("logInsert should log failure") {
    val errorCause = new RuntimeException("Test failure")

    val result = CoreDaosImpl.logInsert[Episode](Throw(errorCause), "episode", record => record.id.toString)

    val _ = result shouldBe a[Throw[?]]
    val _ = result.throwable.getMessage shouldBe errorCause.getMessage
  }

  test("logUpdate should log successful update") {
    val record = FakeNewEpisode

    val result = CoreDaosImpl.logUpdate[Episode](Try(record), "episode", record => record.id.toString)

    val _ = result shouldBe a[Return[?]]
    val _ = result.get() shouldBe record
  }

  test("logUpdate should log failure") {
    val errorCause = new RuntimeException("Test failure")

    val result = CoreDaosImpl.logUpdate[Episode](Throw(errorCause), "episode", record => record.id.toString)

    val _ = result shouldBe a[Throw[?]]
    val _ = result.throwable.getMessage shouldBe errorCause.getMessage
  }

  test("logQuerySeq should log successful query") {
    val records = Seq(1, 2, 3)

    val result = CoreDaosImpl.logQuerySeq(Try(records), "testLabel")

    val _ = result shouldBe a[Return[?]]
    val _ = result.get().size shouldBe records.size
  }

  test("logQuerySeq should log failure") {
    val errorCause = new RuntimeException("Test failure")

    val result = CoreDaosImpl.logQuerySeq(Throw(errorCause), "testLabel")

    val _ = result shouldBe a[Throw[?]]
    val _ = result.throwable.getMessage shouldBe errorCause.getMessage
  }

  test("logQueryOpt should log successful query") {
    val records = Some(42)

    val result = CoreDaosImpl.logQueryOpt(Try(records), "testLabel")

    val _ = result shouldBe a[Return[?]]
    val _ = result.get().size shouldBe records.size
  }

  test("logQueryOpt should log failure") {
    val errorCause = new RuntimeException("Test failure")

    val result = CoreDaosImpl.logQueryOpt(Throw(errorCause), "testLabel")

    val _ = result shouldBe a[Throw[?]]
    val _ = result.throwable.getMessage shouldBe errorCause.getMessage
  }

  test("logDelete should log failure") {
    val errorCause = new RuntimeException("Test failure")

    val result = CoreDaosImpl.logDelete[String](Throw(errorCause), "testLabel", _ => "42")

    val _ = result shouldBe a[Throw[?]]
    val _ = result.throwable.getMessage shouldBe errorCause.getMessage
  }

  private def checkEpisodeTableHasSingleEntry(): Unit = {
    val currentAllEpisodes = TestDao.allEpisodes()
    val _                  = currentAllEpisodes.get().size shouldBe 1
  }

  private def getIdFromEngagementTableWithSingleEntry: Long = {
    val currentAllEngagements = TestDao.allEngagements()
    val _                     = currentAllEngagements.get().size shouldBe 1
    currentAllEngagements.get().head.id
  }

  private def getOnlyEngagementId: Long = {
    val allEngagements = TestDao.allEngagements()
    val _              = allEngagements.get().size shouldBe 1
    allEngagements.get().head.id
  }

}

private object CoreDaosImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")

  private[this] final val TestDatabaseConfig = DatabaseConfig("com.mysql.cj.jdbc.MysqlDataSource",
                                                              "cognifyx-core-test",
                                                              "localhost",
                                                              3306,
                                                              "root",
                                                              "password"
                                                             )

  private[this] def createTestDataSource(config: DatabaseConfig): HikariDataSource = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setDataSourceClassName(config.source)
    hikariConfig.setConnectionTimeout(7000)
    hikariConfig.addDataSourceProperty("serverName", config.host)
    hikariConfig.addDataSourceProperty("portNumber", config.port.toString)
    hikariConfig.addDataSourceProperty("databaseName", config.name)
    hikariConfig.addDataSourceProperty("user", config.username)
    hikariConfig.addDataSourceProperty("password", config.password)
    hikariConfig.setMaximumPoolSize(5)
    hikariConfig.setMinimumIdle(1)

    new HikariDataSource(hikariConfig)
  }

  final val TestDataSource: HikariDataSource = createTestDataSource(TestDatabaseConfig)

  final val TestContext = new CoreSqlDbContext(SnakeCase, TestDataSource)

  final val InitialisationTimeInMs: Long = 500

  object TestDao extends CoreDaosImpl(TestContext) {

    import TestContext.*

    def deleteAllEpisodes(): Unit = {
      val q = quote(schema.episode.delete)
      val _ = run(q)
    }

    def deleteAllTestSessions(): Unit = {
      val q = quote(schema.testSession.delete)
      val _ = run(q)
    }

    def deleteAllUsers(): Unit = {
      val q = quote(schema.users.delete)
      val _ = run(q)
    }

    def deleteAllUserBatches(): Unit = {
      val q = quote(schema.userBatch.delete)
      val _ = run(q)
    }

    def deleteAllEngagements(): Unit = {
      val q = quote(schema.engagements.delete)
      val _ = run(q)
    }

    def deleteAllClients(): Unit = {
      val q = quote(schema.clients.delete)
      val _ = run(q)
    }

    def deleteAllProducts(): Unit = {
      val q = quote(schema.products.delete)
      val _ = run(q)
    }

    def deleteAllUserAccounts(): Unit = {
      val q = quote(schema.userAccount.delete)
      val _ = run(q)
    }

    def deleteAllUserAccountsAud(): Unit = {
      val q = quote(schema.userAccountAud.delete)
      val _ = run(q)
    }

    def deleteAllRevInfo(): Unit = {
      val q = quote(schema.revInfo.delete)
      val _ = run(q)
    }

    def deleteAllUserRoles(): Unit = {
      val q = quote(schema.userRole.delete)
      val _ = run(q)
    }

    def deleteAllCygnusEvents(): Unit = {
      val q = quote(schema.cygnusEvent.delete)
      val _ = run(q)
    }

    def deleteAllSessionOtp(): Unit = {
      val q = quote(schema.sessionOtp.delete)
      val _ = run(q)
    }

    def allUserAccounts(): Seq[UserAccount] = run(query.allUserAccounts)

  }

}

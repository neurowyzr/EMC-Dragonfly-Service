package com.neurowyzr.nw.dragon.service.data.impl

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.cfg.Models.DatabaseConfig
import com.neurowyzr.nw.dragon.service.data.DragonSqlDbContext
import com.neurowyzr.nw.dragon.service.data.impl.Fakes.{FakeUserBatchLookup, FakeUserConsent, FakeUserSurvey}
import com.neurowyzr.nw.dragon.service.data.impl.MyDaosImplTest.{InitialisationTimeInMs, TestDao}
import com.neurowyzr.nw.dragon.service.data.models.{Sample, UserBatchLookup, UserDataConsent, UserSurvey}

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.SnakeCase
import org.scalatest.{BeforeAndAfterAll, OptionValues, TryValues}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class MyDaosImplTest extends AnyFunSuite with Matchers with OptionValues with TryValues with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    Thread.sleep(InitialisationTimeInMs)
    TestDao.deleteAllSamples()
    TestDao.deleteAllUserSurveys()
    TestDao.deleteAllUserDataConsent()
    TestDao.deleteAllUserBatchLookup()

    val tryClientId = TestDao.insertSample(Sample(123L, "new_name", LocalDateTime.now.truncatedTo(ChronoUnit.SECONDS)))
  }

  test("insert sample succeeds") {}

  test("insert survey selections succeeds") {
    TestDao.insertSurveySelections(FakeUserSurvey)
    val allSurveys = TestDao.allUserSurvey()
    val _          = allSurveys.size shouldBe 1
    allSurveys.head.sessionId shouldBe FakeUserSurvey.sessionId
  }

  test("insert consent succeeds") {
    TestDao.insertConsent(FakeUserConsent)
    val allConsents = TestDao.allUserConsent()
    val _           = allConsents.size shouldBe 1
    allConsents.head.userId shouldBe FakeUserConsent.userId
  }

  test("insert user batch lookup succeeds") {
    TestDao.insertNewUserBatchLookup(FakeUserBatchLookup)
    val allUserBatchLookup = TestDao.allUserBatchLookup()
    val _                  = allUserBatchLookup.size shouldBe 1
    allUserBatchLookup.head.key shouldBe FakeUserBatchLookup.key
  }

  test("revoke consent succeeds") {
    TestDao.revokeConsentByUserId(FakeUserConsent.userId)
    val allConsents = TestDao.allUserConsent()
    val _           = allConsents.size shouldBe 1
    allConsents.head.userId shouldBe FakeUserConsent.userId
    allConsents.head.isConsent shouldBe false
    allConsents.head.maybeUtcRevokedAt.isDefined shouldBe true
  }

  test("get user consent by user id succeeds") {
    val tryConsent = TestDao.getUserConsentByUserId(FakeUserConsent.userId)
    tryConsent.asScala.success.value.value.userId shouldBe FakeUserConsent.userId
  }

  test("get user batch lookup by key succeeds") {
    val tryLookup = TestDao.getUserBatchLookupByKey(FakeUserBatchLookup.key)
    tryLookup.asScala.success.value.value.value shouldBe FakeUserBatchLookup.value
  }

  test("get user batch lookup by code succeeds") {
    val tryLookup = TestDao.getUserBatchLookupByCode(FakeUserBatchLookup.value)
    tryLookup.asScala.success.value.value.key shouldBe FakeUserBatchLookup.key
  }

}

private object MyDaosImplTest {
  final val TestException: Exception = new Exception("Exception was thrown")

  private[this] final val TestDatabaseConfig = DatabaseConfig("com.mysql.cj.jdbc.MysqlDataSource",
                                                              "nw-apollo-dragon-service-db-test",
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

  final val TestContext = new DragonSqlDbContext(SnakeCase, TestDataSource)

  final val InitialisationTimeInMs: Long = 500

  object TestDao extends MyDaosImpl(TestContext) {

    import TestContext.*

    def deleteAllSamples(): Unit = {
      val q = quote(schemaExtra.sample.delete)
      val _ = run(q)
    }

    def deleteAllUserSurveys(): Unit = {
      val q = quote(schemaExtra.userSurvey.delete)
      val _ = run(q)
    }

    def deleteAllUserDataConsent(): Unit = {
      val q = quote(schemaExtra.userDataConsent.delete)
      val _ = run(q)
    }

    def deleteAllUserBatchLookup(): Unit = {
      val q = quote(schemaExtra.userBatchLookup.delete)
      val _ = run(q)
    }

    def insertNewUserBatchLookup(entity: UserBatchLookup): Unit = run(query.insertNewUserBatchLookup(entity))

    def allUserSurvey(): Seq[UserSurvey]           = run(query.allUserSurvey)
    def allUserConsent(): Seq[UserDataConsent]     = run(query.allUserDataConsent)
    def allUserBatchLookup(): Seq[UserBatchLookup] = run(query.allUserBatchLookup)

  }

}

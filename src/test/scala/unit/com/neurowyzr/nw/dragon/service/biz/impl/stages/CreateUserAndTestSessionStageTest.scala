package com.neurowyzr.nw.dragon.service.biz.impl.stages

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.util.{Await, Future}

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeDbfsConfig
import com.neurowyzr.nw.dragon.service.biz.impl.Fakes.FakeUser
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.CreateUserStage
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.PatientExist
import com.neurowyzr.nw.dragon.service.cfg.Models.DbfsConfig
import com.neurowyzr.nw.dragon.service.data.UserRepository

import com.github.t3hnar.bcrypt.*
import com.softwaremill.quicklens.ModifyPimp
import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CreateUserAndTestSessionStageTest
    extends AnyWordSpec with IdiomaticMockito with Matchers with ResetMocksAfterEachTest {

  val mockRepo: UserRepository = mock[UserRepository]
  val mockConfig: DbfsConfig   = FakeDbfsConfig
  private val testInstance     = new CreateUserStage(mockRepo, mockConfig)

  "outcome error if user is found" in {
    val invalidTask = FakeCreateUserTask.modify(_.out.userId).setTo(Some(1234L))

    val result = Await.result(testInstance.execute(invalidTask), 1.second)

    val _ = result.out.maybeOutcome shouldBe Some(PatientExist)
  }

  "outcome success continue if user is created" in {
    val currentTask = FakeCreateUserTask.modify(_.out.userId).setTo(None)
    val _ = mockRepo.createUser(*[com.neurowyzr.nw.dragon.service.biz.models.User]) returns Future.value(FakeUser)

    val result = Await.result(testInstance.execute(currentTask), 1.second)

    val _ = result.out.maybeOutcome shouldBe Some(Outcomes.Success)
  }

  "create new user with the correct properties" in {
    val defaultPassword = "defaultPassword"

    val newUser = CreateUserStage.createNewUser(FakeCreateUserTask, defaultPassword)

    val _ = newUser.username shouldBe s"${FakeCreateUserTask.in.source}_${FakeCreateUserTask.in.patientId}"
    val _ = newUser.password should not be empty
    val _ = newUser.firstName shouldBe s"${FakeCreateUserTask.in.source}_${FakeCreateUserTask.in.patientId}"
    val _ = newUser.maybeExternalPatientRef shouldBe Some(FakeCreateUserTask.in.patientId)
  }

  "generatePrefix" should {
    "return fixed length for shorter input" when {
      "length is less than 3" in {
        val input           = "ab"
        val generatedPrefix = CreateUserStage.generatePrefix(input)

        val _ = generatedPrefix.length shouldBe 3
        val _ = generatedPrefix shouldBe "ab_"
      }
      "length is equal to 3" in {
        val input           = "abc"
        val generatedPrefix = CreateUserStage.generatePrefix(input)

        val _ = generatedPrefix.length shouldBe 3
        val _ = generatedPrefix shouldBe "abc"
      }
      "length is more than 3" in {
        val input           = "abcdefg"
        val generatedPrefix = CreateUserStage.generatePrefix(input)

        val _ = generatedPrefix.length shouldBe 3
        val _ = generatedPrefix shouldBe "abc"
      }
    }
  }

  "generateSuffix" should {
    "truncate" when {
      "length is greater than target length" in {
        val input           = "01234567890123456789abcdefghijklmnopqrstuvwxyz"
        val generatedSuffix = CreateUserStage.generateSuffix(input)

        val _ = generatedSuffix.length shouldBe 20
        val _ = generatedSuffix shouldBe "01234567890123456789"
      }
    }
    "return the same length as input" when {
      "length is less than target length" in {
        val input           = "0123456789012345"
        val generatedSuffix = CreateUserStage.generateSuffix(input)

        val _ = generatedSuffix.length shouldBe 16
        val _ = generatedSuffix shouldBe "0123456789012345"
      }
    }
  }

  "generatePasswordHash" should {
    "generate a hash" in {
      val password = "password"

      val passwordHash = CreateUserStage.generatePasswordHash(password)

      val _ = password.isBcryptedBounded(passwordHash) shouldBe true
    }
  }

}

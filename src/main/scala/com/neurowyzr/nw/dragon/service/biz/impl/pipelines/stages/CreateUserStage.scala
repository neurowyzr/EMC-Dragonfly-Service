package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages.CreateUserStage.createNewUser
import com.neurowyzr.nw.dragon.service.biz.models.*
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.PatientExist
import com.neurowyzr.nw.dragon.service.cfg.Models.DbfsConfig
import com.neurowyzr.nw.dragon.service.data.UserRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.github.t3hnar.bcrypt.*
import com.softwaremill.quicklens.ModifyPimp

private[impl] class CreateUserStage(userRepository: UserRepository, dbfsConfig: DbfsConfig)
    extends FStage[CreateUserTask] {

  override def execute(task: CreateUserTask): Future[CreateUserTask] = {
    task.out.userId match {
      case Some(_) => Future.value(task.modify(_.out.maybeOutcome).setTo(Some(PatientExist)))
      case None =>
        val newUser = createNewUser(task, dbfsConfig.newUserDefaultPassword.value)
        userRepository
          .createUser(newUser)
          .map(_ =>
            task.modify(_.out.userId).setTo(Some(newUser.id)).modify(_.out.maybeOutcome).setTo(Some(Outcomes.Success))
          )
    }
  }

}

private[impl] object CreateUserStage {
  private val PrefixLength = 3
  private val SuffixLength = 20

  def createNewUser(task: CreateUserTask, defaultPassword: String): User = {
    val username = task.in.source + "_" + task.in.patientId

    User(username, generatePasswordHash(defaultPassword), username, task.in.source, task.in.patientId)
  }

  def generatePrefix(input: String): String = {

    if (input.length >= PrefixLength) {
      input.substring(0, PrefixLength)
    } else {
      // Append underscores until the length becomes target length
      input + "_" * (PrefixLength - input.length)
    }
  }

  def generateSuffix(input: String): String = {

    if (input.length >= SuffixLength) {
      input.substring(0, SuffixLength)
    } else {
      input
    }
  }

  def generatePasswordHash(password: String): String = {
    val salt           = generateSalt
    val hashedPassword = password.bcryptBounded(salt)

    hashedPassword
  }

}

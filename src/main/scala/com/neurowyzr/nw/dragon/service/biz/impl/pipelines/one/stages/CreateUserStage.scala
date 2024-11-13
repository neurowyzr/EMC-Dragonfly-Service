package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import javax.inject.Inject

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.CreateTestSessionException
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages.CreateUserStage.generatePasswordHash
import com.neurowyzr.nw.dragon.service.biz.models.{CreateTestSessionTask, Outcomes, User}
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus.UserCreationFailure
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.PatientRefExist
import com.neurowyzr.nw.dragon.service.cfg.Models.{CustomerConfig, DbfsConfig}
import com.neurowyzr.nw.dragon.service.data.UserRepository
import com.neurowyzr.nw.dragon.service.utils.context.EncryptionUtil
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.github.t3hnar.bcrypt.*
import com.github.t3hnar.bcrypt.generateSalt
import com.softwaremill.quicklens.ModifyPimp

private[impl] class CreateUserStage @Inject() (userRepo: UserRepository,
                                               dbfsConfig: DbfsConfig,
                                               customerConfig: CustomerConfig
                                              )
    extends FStage[CreateTestSessionTask] {

  override def execute(task: CreateTestSessionTask): Future[CreateTestSessionTask] = {
    val patientRef = task.in.patientRef
    val source     = task.in.source
    val requestId  = task.in.requestId

    userRepo
      .getUserBySourceAndExternalPatientRef(source, patientRef)
      .flatMap {
        case Some(user) =>
          Future.value(
            task.modify(_.out.maybeUserId).setTo(Some(user.id)).modify(_.out.maybeOutcome).setTo(Some(PatientRefExist))
          )
        case None =>
          val newUser = createNewClientUser(task, dbfsConfig.newUserDefaultPassword.value)
          userRepo
            .createUser(newUser)
            .map((createdUser: User) =>
              task
                .modify(_.out.maybeUserId)
                .setTo(Some(createdUser.id))
                .modify(_.out.maybeOutcome)
                .setTo(Some(Outcomes.PatientRefNotFound))
            )
      }
      .rescue { case e: Exception => abort(CreateTestSessionException(UserCreationFailure, e.getMessage)) }
  }

  private def createNewClientUser(task: CreateTestSessionTask, defaultPassword: String): User = {
    User(
      username = task.in.patientRef,
      firstName = task.in.firstName,
      lastName = task.in.lastName,
      maybeEmailHashed = task.in.maybeEmail.map(EncryptionUtil.aesEncrypt(_)),
      maybeMobile = task.in.maybeMobileNumber,
      dob = task.in.birthDate,
      gender = task.in.gender,
      password = generatePasswordHash(defaultPassword),
      source = customerConfig.source,
      externalPatientRef = task.in.patientRef
    )
  }

}

private[impl] object CreateUserStage {

  def generatePasswordHash(password: String): String = {
    val salt           = generateSalt
    val hashedPassword = password.bcryptBounded(salt)

    hashedPassword
  }

}

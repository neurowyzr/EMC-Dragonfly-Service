package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.ErrorOutcomeException
import com.neurowyzr.nw.dragon.service.biz.models.CreateMagicLinkTask
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.PatientNotFound
import com.neurowyzr.nw.dragon.service.data.UserRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.softwaremill.quicklens.*

private[impl] class VerifyUserExistStage(userRepo: UserRepository) extends FStage[CreateMagicLinkTask] {

  override def execute(task: CreateMagicLinkTask): Future[CreateMagicLinkTask] = {
    userRepo.getUserByExternalPatientRef(task.in.patientId).flatMap {
      case Some(user) => Future.value(task.modify(_.out.userId).setTo(Some(user.id)))
      case _ =>
        val reason = s"Patient ref: ${task.in.patientId} doesn't existed in User table"
        abort(ErrorOutcomeException(PatientNotFound, reason))
    }
  }

}

package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.stages

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.models.CreateUserTask
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.PatientRefExist
import com.neurowyzr.nw.dragon.service.data.UserRepository
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.softwaremill.quicklens.*

private[impl] class VerifyPatientRefExistStage(userRepository: UserRepository) extends FStage[CreateUserTask] {

  override def execute(task: CreateUserTask): Future[CreateUserTask] = {
    userRepository.getUserBySourceAndExternalPatientRef(task.in.source, task.in.patientId).flatMap {
      case Some(user) =>
        val updatedTask = task
          .modify(_.out.userId)
          .setTo(Some(user.id))
          .modify(_.out.maybeOutcome)
          .setTo(Some(PatientRefExist))
        Future.apply(updatedTask)
      case _ => skip(task)
    }
  }

}

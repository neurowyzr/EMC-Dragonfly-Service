package com.neurowyzr.nw.dragon.service.biz.models

import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.{Error, Success}

sealed trait Outcome extends Serializable {

  override def toString: String =
    this match {
      case Success  => "Success"
      case Error(_) => "Error"
    }

  def getErrorCode: String =
    this match {
      case Success               => ""
      case Error(errorCode: Int) => errorCode.toString
    }

}

object Outcomes {
  case object Success extends Outcome

  sealed case class Error(errorCode: Int) extends Outcome

  object PatientNotFound      extends Error(101)
  object UserBatchCodeMissing extends Error(102)
  object EngagementNotFound   extends Error(103)
  object EpisodeNotFound      extends Error(106)
  object PatientRefExist      extends Error(107)
  object PatientExist         extends Error(108)
  object PatientRefNotFound   extends Error(109)
}

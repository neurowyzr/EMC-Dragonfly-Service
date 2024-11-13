package com.neurowyzr.nw.dragon.service.biz.models

import java.time.LocalDateTime
import java.util.Date

sealed trait TaskInput

sealed trait TaskOutput {
  def maybeOutcome: Option[Outcome]
}

sealed trait Task {
  type Input <: TaskInput
  type Output <: TaskOutput
  def ctx: TaskContext
  def in: Input
  def out: Output
}

final case class TaskContext(
    maybeTimestamp: Option[Date],
    maybeAppId: Option[String],
    maybeType: Option[String],
    maybeMessageId: Option[String],
    maybeExpiration: Option[String],
    maybeCorrelationId: Option[String]
) {
  def appId: String = maybeAppId.getOrElse("")

  def `type`: String = maybeType.getOrElse("")

  def messageId: String = maybeMessageId.getOrElse("")

  def expiration: String    = maybeExpiration.getOrElse("")
  def correlationId: String = maybeCorrelationId.getOrElse("")
}

object TaskContext {
  def apply(): TaskContext = TaskContext(None, None, None, None, None, None)
}

final case class UploadReportTaskInput(
    episodeId: Long,
    userBatchCode: String,
    s3Urls: Seq[String] // sequence of string (urls of the document in s3) write a function to select url from the list - for now pick the first item
) extends TaskInput

final case class UploadReportTaskOutput(maybeRequestId: Option[String],
                                        maybeEpisodeRef: Option[String],
                                        maybeUserId: Option[Long],
                                        maybePatientRef: Option[String],
                                        maybeLocationId: Option[String],
                                        maybeReport: Option[AttachmentOutput],
                                        maybeOutcome: Option[Outcome]
                                       )
    extends TaskOutput

object UploadReportTaskOutput {
  def apply(): UploadReportTaskOutput = UploadReportTaskOutput(None, None, None, None, None, None, None)
}

final case class UploadReportTask(ctx: TaskContext, in: UploadReportTaskInput, out: UploadReportTaskOutput) extends Task {
  type Input  = UploadReportTaskInput
  type Output = UploadReportTaskOutput
}

object UploadReportTask {

  def apply(context: TaskContext, input: UploadReportTaskInput): UploadReportTask = UploadReportTask(
    context,
    input,
    UploadReportTaskOutput()
  )

}

final case class AttachmentOutput(
    bytestream: Array[Byte],
    name: String
)

final case class NotifyClientTaskInput(requestId: String,
                                       patientRef: String,
                                       episodeRef: String,
                                       locationId: String,
                                       outcome: Int,
                                       maybeMagicLink: Option[String]
                                      )
    extends TaskInput

final case class NotifyClientTaskOutput(maybeOutcome: Option[Outcome]) extends TaskOutput

object NotifyClientTaskOutput {
  def apply(): NotifyClientTaskOutput = NotifyClientTaskOutput(None)
}

final case class NotifyClientTask(ctx: TaskContext, in: NotifyClientTaskInput, out: NotifyClientTaskOutput) extends Task {
  type Input  = NotifyClientTaskInput
  type Output = NotifyClientTaskOutput
}

object NotifyClientTask {

  def apply(context: TaskContext, input: NotifyClientTaskInput): NotifyClientTask = NotifyClientTask(
    context,
    input,
    NotifyClientTaskOutput()
  )

}

final case class CreateTestSessionTaskInput(requestId: String,
                                            patientRef: String,
                                            episodeRef: String,
                                            locationId: String,
                                            firstName: String,
                                            lastName: String,
                                            birthDate: String,
                                            gender: String,
                                            source: String,
                                            startDate: LocalDateTime,
                                            expiryDate: LocalDateTime,
                                            maybeEmail: Option[String],
                                            maybeMobileNumber: Option[Long]
                                           )
    extends TaskInput

final case class CreateTestSessionTaskOutput(maybeUserId: Option[Long],
                                             maybeUserBatchId: Option[Long],
                                             maybeUserBatchCode: Option[String],
                                             maybeEngagementId: Option[Long],
                                             maybeMagicLinkUrl: Option[String],
                                             maybeOutcome: Option[Outcome]
                                            )
    extends TaskOutput

object CreateTestSessionTaskOutput {
  def apply(): CreateTestSessionTaskOutput = CreateTestSessionTaskOutput(None, None, None, None, None, None)
}

final case class CreateTestSessionTask(
    ctx: TaskContext,
    in: CreateTestSessionTaskInput,
    out: CreateTestSessionTaskOutput
) extends Task {
  type Input  = CreateTestSessionTaskInput
  type Output = CreateTestSessionTaskOutput
}

object CreateTestSessionTask {

  def apply(context: TaskContext, input: CreateTestSessionTaskInput): CreateTestSessionTask = CreateTestSessionTask(
    context,
    input,
    CreateTestSessionTaskOutput()
  )

}

final case class CreateMagicLinkTaskInput(source: String,
                                          userBatchCode: String,
                                          patientId: String,
                                          testId: String,
                                          startDate: LocalDateTime,
                                          expiryDate: LocalDateTime
                                         )
    extends TaskInput

final case class CreateMagicLinkTaskOutput(userId: Option[Long], userBatchId: Option[Long], maybeOutcome: Option[Outcome])
    extends TaskOutput

final case class CreateMagicLinkTask(
    ctx: TaskContext,
    in: CreateMagicLinkTaskInput,
    out: CreateMagicLinkTaskOutput
) extends Task {
  type Input  = CreateMagicLinkTaskInput
  type Output = CreateMagicLinkTaskOutput
}

object CreateMagicLinkTaskOutput {
  def apply(): CreateMagicLinkTaskOutput = CreateMagicLinkTaskOutput(None, None, None)
}

object CreateMagicLinkTask {

  def apply(context: TaskContext, input: CreateMagicLinkTaskInput): CreateMagicLinkTask = CreateMagicLinkTask(
    context,
    input,
    CreateMagicLinkTaskOutput()
  )

}

final case class CreateUserTaskInput(source: String, patientId: String) extends TaskInput

final case class CreateUserTaskOutput(userId: Option[Long], maybeOutcome: Option[Outcome]) extends TaskOutput

final case class CreateUserTask(
    ctx: TaskContext,
    in: CreateUserTaskInput,
    out: CreateUserTaskOutput
) extends Task {
  type Input  = CreateUserTaskInput
  type Output = CreateUserTaskOutput
}

object CreateUserTaskOutput {
  def apply(): CreateUserTaskOutput = CreateUserTaskOutput(None, None)
}

object CreateUserTask {

  def apply(context: TaskContext, input: CreateUserTaskInput): CreateUserTask = CreateUserTask(
    context,
    input,
    CreateUserTaskOutput()
  )

}

final case class UpdateMagicLinkTaskInput(testId: String, expiryDate: LocalDateTime) extends TaskInput

final case class UpdateMagicLinkTaskOutput(maybeOutcome: Option[Outcome]) extends TaskOutput

final case class UpdateMagicLinkTask(
    ctx: TaskContext,
    in: UpdateMagicLinkTaskInput,
    out: UpdateMagicLinkTaskOutput
) extends Task {
  type Input  = UpdateMagicLinkTaskInput
  type Output = UpdateMagicLinkTaskOutput
}

object UpdateMagicLinkTaskOutput {
  def apply(): UpdateMagicLinkTaskOutput = UpdateMagicLinkTaskOutput(None)
}

object UpdateMagicLinkTask {

  def apply(context: TaskContext, input: UpdateMagicLinkTaskInput): UpdateMagicLinkTask = UpdateMagicLinkTask(
    context,
    input,
    UpdateMagicLinkTaskOutput()
  )

}

final case class InvalidateMagicLinkTaskInput(testId: String) extends TaskInput

final case class InvalidateMagicLinkTaskOutput(maybeOutcome: Option[Outcome]) extends TaskOutput

final case class InvalidateMagicLinkTask(
    ctx: TaskContext,
    in: InvalidateMagicLinkTaskInput,
    out: InvalidateMagicLinkTaskOutput
) extends Task {
  type Input  = InvalidateMagicLinkTaskInput
  type Output = InvalidateMagicLinkTaskOutput
}

object InvalidateMagicLinkTaskOutput {
  def apply(): InvalidateMagicLinkTaskOutput = InvalidateMagicLinkTaskOutput(None)
}

object InvalidateMagicLinkTask {

  def apply(context: TaskContext, input: InvalidateMagicLinkTaskInput): InvalidateMagicLinkTask =
    InvalidateMagicLinkTask(
      context,
      input,
      InvalidateMagicLinkTaskOutput()
    )

}

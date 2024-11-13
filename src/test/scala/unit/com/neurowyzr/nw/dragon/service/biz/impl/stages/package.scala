package com.neurowyzr.nw.dragon.service.biz.impl

import java.util.Date

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeNewEpisodeRef
import com.neurowyzr.nw.dragon.service.biz.models.*
import com.neurowyzr.nw.dragon.service.biz.models.Defaults.LocalDateTimeNow
import com.neurowyzr.nw.dragon.service.mq.{
  CreateMagicLinkCmd, CreateUserCmd, InvalidateMagicLinkCmd, NotifyClientTaskCmd, UpdateMagicLinkCmd, UploadCmd
}

package object stages {

  final val FakeUploadReportTaskInput = UploadReportTaskInput(
    12345,
    "fake-user-batch-code",
    Seq("url")
  )

  final val FakeUploadReportTaskContext = TaskContext(
    Some(new Date()),
    Some("appId"),
    Some(UploadCmd.toString),
    Some("messageId"),
    Some("expiration"),
    Some("correlation")
  )

  final val FakeUploadReportTask = UploadReportTask(FakeUploadReportTaskContext, FakeUploadReportTaskInput)

  final val FakeNotifyClientTaskContext = TaskContext(
    Some(new Date()),
    Some("appId"),
    Some(NotifyClientTaskCmd.toString),
    Some("messageId"),
    Some("expiration"),
    Some("correlation")
  )

  final val FakeNotifyClientTaskInput = NotifyClientTaskInput(
    "fake-request-id",
    "fake-patient-ref",
    "fake-episode-ref",
    "fake-location-id",
    1,
    Some("fake-magic-link")
  )

  final val FakeNotifyClientTask = NotifyClientTask(FakeNotifyClientTaskContext, FakeNotifyClientTaskInput)

  final val FakeCreateTestSessionTaskContext = TaskContext(
    Some(new Date()),
    Some("appId"),
    Some(CreateMagicLinkCmd.toString),
    Some("messageId"),
    Some("expiration"),
    Some("correlation")
  )

  final val FakeCreateTestSessionTaskInput = CreateTestSessionTaskInput(
    "fake-request-id",
    "fake-patient-id",
    "fake-episode-id",
    "fake-location-id",
    "fake-first-name",
    "fake-last-name",
    "fake-dob",
    "fake-gender",
    "fake-source",
    LocalDateTimeNow,
    LocalDateTimeNow,
    Some("fake-email"),
    Some(123456789L)
  )

  final val FakeCreateTestSessionTask = CreateTestSessionTask(FakeCreateTestSessionTaskContext,
                                                              FakeCreateTestSessionTaskInput
                                                             )

  final val FakeCreateMagicLinkTaskContext = TaskContext(
    Some(new Date()),
    Some("appId"),
    Some(CreateMagicLinkCmd.toString),
    Some("messageId"),
    Some("expiration"),
    Some("correlation")
  )

  final val FakeCreateMagicLinkTaskInput = CreateMagicLinkTaskInput("fake-source",
                                                                    "fake-code",
                                                                    "fake-patient-id",
                                                                    "fake-test-id",
                                                                    LocalDateTimeNow,
                                                                    LocalDateTimeNow
                                                                   )

  final val FakeCreateMagicLinkTask = CreateMagicLinkTask(FakeCreateMagicLinkTaskContext, FakeCreateMagicLinkTaskInput)

  final val FakeCreateUserTaskContext = TaskContext(Some(new Date()),
                                                    Some("appId"),
                                                    Some(CreateUserCmd.toString),
                                                    Some("messageId"),
                                                    Some("expiration"),
                                                    Some("correlation")
                                                   )

  final val FakeCreateUserTaskInput = CreateUserTaskInput("fake-source", "fake-patient-id")

  final val FakeCreateUserTask = CreateUserTask(FakeCreateUserTaskContext, FakeCreateUserTaskInput)

  final val FakeUpdateMagicLinkTaskContext = TaskContext(
    Some(new Date()),
    Some("appId"),
    Some(UpdateMagicLinkCmd.toString),
    Some("messageId"),
    Some("expiration"),
    Some("correlation")
  )

  final val FakeUpdateMagicLinkTaskInput = UpdateMagicLinkTaskInput(FakeNewEpisodeRef, LocalDateTimeNow.plusDays(1))

  final val FakeUpdateMagicLinkTask = UpdateMagicLinkTask(FakeUpdateMagicLinkTaskContext, FakeUpdateMagicLinkTaskInput)

  final val FakeInvalidateMagicLinkTaskContext = TaskContext(
    Some(new Date()),
    Some("appId"),
    Some(InvalidateMagicLinkCmd.toString),
    Some("messageId"),
    Some("expiration"),
    Some("correlation")
  )

  final val FakeInvalidateMagicLinkTaskInput = InvalidateMagicLinkTaskInput(FakeNewEpisodeRef)

  final val FakeInvalidateMagicLinkTask = InvalidateMagicLinkTask(FakeInvalidateMagicLinkTaskContext,
                                                                  FakeInvalidateMagicLinkTaskInput
                                                                 )

}

package com.neurowyzr.nw.dragon.service.mq

import java.time.LocalDateTime

import com.fasterxml.jackson.annotation.JsonProperty

private[mq] object Models {

  sealed trait Command

  final case class UploadCommand(episodeId: Long, userBatchCode: String, s3Urls: Seq[String])

  final case class NotifyClientCommand(requestId: String,
                                       patientRef: String,
                                       episodeRef: String,
                                       locationId: String,
                                       outcome: Int,
                                       @JsonProperty("magic_link_url") maybeMagicLink: Option[String]
                                      )
      extends Command

  final case class CreateTestSessionCommand(requestId: String,
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
      extends Command

  final case class CreateMagicLinkCommand(userBatchCode: String,
                                          testId: String,
                                          patientId: String,
                                          startDate: LocalDateTime,
                                          expiryDate: LocalDateTime,
                                          source: String
                                         )
      extends Command

  final case class CreateUserCommand(source: String, patientId: String)              extends Command
  final case class UpdateMagicLinkCommand(testId: String, expiryDate: LocalDateTime) extends Command
  final case class InvalidateMagicLinkCommand(testId: String)                        extends Command

}

package com.neurowyzr.nw.dragon.service.mq

import com.fasterxml.jackson.annotation.JsonProperty

sealed trait Command extends Serializable {
  override def toString: String = this.getClass.getSimpleName.dropRight(1)

  def ackName: String = this.toString.replace("Cmd", "Ack")
}

case object UploadCmd              extends Command
case object NotifyClientTaskCmd    extends Command
case object CreateTestSessionCmd   extends Command
case object CreateMagicLinkCmd     extends Command
case object CreateUserCmd          extends Command
case object UpdateMagicLinkCmd     extends Command
case object InvalidateMagicLinkCmd extends Command

final case class Ack(command: String, outcome: String, errorCode: String)

final case class EmailCommand(
    template: String,
    jsonSettings: String,
    @JsonProperty("mailing_list") maybeMailingList: Option[String],
    @JsonProperty("to") maybeTo: Option[Set[String]],
    @JsonProperty("locale") maybeLocale: Option[String],
    @JsonProperty("sender_name") maybeSenderName: Option[String],
    @JsonProperty("cc") maybeCc: Option[Set[String]],
    @JsonProperty("bcc") maybeBcc: Option[Set[String]],
    @JsonProperty("attachment_urls") maybeAttachmentUrls: Option[Set[String]],
    @JsonProperty("archive_name") maybeArchiveName: Option[String],
    @JsonProperty("archive_password") maybeArchivePassword: Option[String]
)

object EmailCommand {

  def apply(template: String, jsonSettings: String, to: Set[String]): EmailCommand = EmailCommand(
    template,
    jsonSettings,
    None,
    Some(to),
    None,
    None,
    None,
    None,
    None,
    None,
    None
  )

}

private object Command {

  def getCommand(commandStr: String): Option[Command] =
    commandStr match {
      case "UploadCmd"              => Some(UploadCmd)
      case "NotifyClientTaskCmd"    => Some(NotifyClientTaskCmd)
      case "CreateTestSessionCmd"   => Some(CreateTestSessionCmd)
      case "CreateMagicLinkCmd"     => Some(CreateMagicLinkCmd)
      case "CreateUserCmd"          => Some(CreateUserCmd)
      case "UpdateMagicLinkCmd"     => Some(UpdateMagicLinkCmd)
      case "InvalidateMagicLinkCmd" => Some(InvalidateMagicLinkCmd)
      case _                        => None
    }

}

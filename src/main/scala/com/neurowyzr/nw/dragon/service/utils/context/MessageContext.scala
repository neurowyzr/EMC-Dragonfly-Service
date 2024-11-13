package com.neurowyzr.nw.dragon.service.utils.context

import com.twitter.finagle.context.Contexts

import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.Message

final case class MessageContext(message: Message) {

  private final val map = message.maybeProperties
    .map { properties =>
      Seq(
        properties.maybeMessageId.map("message_id" -> _),
        properties.maybeAppId.map("message_origin" -> _),
        properties.maybeType.map("message_type" -> _),
        properties.maybeTimestamp.map("message_timestamp" -> _.toString)
      ).collect { case Some(tuple) => tuple }.toMap
    }
    .getOrElse(Map.empty)

  def toMap: Map[String, String] = map

}

object MessageContext {
  final val MessageContextKey: Contexts.local.Key[MessageContext] = Contexts.local.newKey[MessageContext]()

  def toMap: Map[String, String] = Contexts.local.get(MessageContextKey).map(_.toMap).getOrElse(Map.empty)
}

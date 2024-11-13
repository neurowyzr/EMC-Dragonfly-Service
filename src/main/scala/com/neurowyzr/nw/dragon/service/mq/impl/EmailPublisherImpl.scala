package com.neurowyzr.nw.dragon.service.mq.impl

import java.util.Date
import java.util.UUID.randomUUID
import javax.inject.Inject

import com.twitter.util.{Duration, Future}
import com.twitter.util.jackson.ScalaObjectMapper
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.biz.models.EmailOtpArgs
import com.neurowyzr.nw.dragon.service.di.annotations.EmailPublisherBinding
import com.neurowyzr.nw.dragon.service.mq.{EmailCommand, EmailPublisher}
import com.neurowyzr.nw.dragon.service.mq.impl.EmailPublisherImpl.ExpirationInMs
import com.neurowyzr.nw.finatra.lib.cfg.Models.AppInfo
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.{Message, PersistentMsgProperties}
import com.neurowyzr.nw.finatra.rabbitmq.lib.MqContext
import com.neurowyzr.nw.finatra.rabbitmq.lib.cfg.Models.PublisherConfig

import com.google.inject.Singleton

@Singleton
class EmailPublisherImpl @Inject() (mapper: ScalaObjectMapper,
                                    ctx: MqContext,
                                    @EmailPublisherBinding config: PublisherConfig,
                                    appInfo: AppInfo
                                   )
    extends Logging with EmailPublisher {

  override def publishOtpEmail(emailOtpArgs: EmailOtpArgs, emailTo: Set[String]): Future[Unit] = {
    val payload    = mapper.writeValueAsString(emailOtpArgs)
    val command    = EmailCommand("send_otp_dragon", payload, emailTo)
    val commandStr = mapper.writeValueAsString(command)
    val properties = PersistentMsgProperties(
      maybeAppId = Some(appInfo.name),
      maybeMessageId = Some(randomUUID.toString),
      maybeExpiration = Some(ExpirationInMs),
      maybeType = Some("EmailCmd"),
      maybeTimestamp = Some(new Date()),
      maybeCorrelationId = None,
      headers = Map.empty
    )
    val publisher = ctx.createDefaultPublisher(config)
    publisher.publish(Message(commandStr, properties)).ensure(publisher.close())
  }

}

private object EmailPublisherImpl {

  final val ExpirationInMs = Duration.fromDays(10).inMilliseconds.toString

}

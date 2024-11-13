package com.neurowyzr.nw.dragon.service.mq.impl

import java.util.Date
import javax.inject.Inject

import com.twitter.util.Future
import com.twitter.util.jackson.ScalaObjectMapper
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.biz.models.{Outcome, TaskContext}
import com.neurowyzr.nw.dragon.service.di.annotations.CygnusPublisherBinding
import com.neurowyzr.nw.dragon.service.mq.{Ack, Command, CygnusPublisher}
import com.neurowyzr.nw.finatra.lib.cfg.Models.AppInfo
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.{Message, PersistentMsgProperties}
import com.neurowyzr.nw.finatra.rabbitmq.lib.MqContext
import com.neurowyzr.nw.finatra.rabbitmq.lib.cfg.Models.PublisherConfig

import com.google.inject.Singleton

@Singleton
class CygnusPublisherImpl @Inject() (mapper: ScalaObjectMapper,
                                     ctx: MqContext,
                                     @CygnusPublisherBinding config: PublisherConfig,
                                     appInfo: AppInfo
                                    )
    extends Logging with CygnusPublisher {

  def publishOutcome(context: TaskContext, outcome: Outcome): Future[Unit] = {
    val maybeCommand: Option[Command] = Command.getCommand(context.`type`)

    val ack: Ack = Ack(context.`type`, outcome.toString, outcome.getErrorCode)
    val payload  = mapper.writeValueAsString(ack)

    maybeCommand match {
      case Some(command) =>
        val properties = PersistentMsgProperties(
          maybeAppId = Some(appInfo.name),
          maybeMessageId = context.maybeMessageId,
          maybeExpiration = context.maybeExpiration,
          maybeType = Some(command.ackName),
          maybeTimestamp = Some(new Date()),
          maybeCorrelationId = context.maybeCorrelationId,
          headers = Map.empty
        )

        val publisher = ctx.createDefaultPublisher(config)
        publisher.publish(Message(payload, properties)).ensure(publisher.close())
      case None => Future.Done
    }

  }

}

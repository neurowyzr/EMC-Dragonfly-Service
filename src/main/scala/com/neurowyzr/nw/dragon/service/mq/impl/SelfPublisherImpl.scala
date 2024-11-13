package com.neurowyzr.nw.dragon.service.mq.impl

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.inject.Inject

import com.twitter.util.{Duration, Future}
import com.twitter.util.jackson.ScalaObjectMapper
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.biz.exceptions.PublisherException
import com.neurowyzr.nw.dragon.service.biz.models.{CreateMagicLinkTaskInput, CreateTestSessionTask, Outcome, TaskContext}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.EnqueueTestSessionParams
import com.neurowyzr.nw.dragon.service.cfg.Models.{CustomerConfig, DbfsConfig}
import com.neurowyzr.nw.dragon.service.di.annotations.SelfPublisherBinding
import com.neurowyzr.nw.dragon.service.mq.{
  Command, CreateTestSessionCmd, NotifyClientTaskCmd, QueueConsumer, SelfPublisher
}
import com.neurowyzr.nw.dragon.service.mq.Models.CreateTestSessionCommand
import com.neurowyzr.nw.dragon.service.mq.impl.SelfPublisherImpl.{
  generateMsgProperties, transformEnqueueTestSessionParamstoPayload, transformPublishParamsIntoFailurePayload,
  transformPublishParamsIntoSuccessPayload, ExpirationInMs, PublishFailurePayload, PublishParams, PublishSuccessPayload
}
import com.neurowyzr.nw.finatra.lib.cfg.Models.AppInfo
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.{Message, PersistentMsgProperties}
import com.neurowyzr.nw.finatra.rabbitmq.lib.MqContext
import com.neurowyzr.nw.finatra.rabbitmq.lib.cfg.Models.PublisherConfig

import com.google.inject.Singleton
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.TransformationOps

@Singleton
class SelfPublisherImpl @Inject() (mapper: ScalaObjectMapper,
                                   ctx: MqContext,
                                   @SelfPublisherBinding config: PublisherConfig,
                                   appInfo: AppInfo,
                                   customerConfig: CustomerConfig,
                                   queueConsumer: QueueConsumer,
                                   dbfsConfig: DbfsConfig
                                  )
    extends Logging with SelfPublisher {

  override def publishTestSessionMessage(params: EnqueueTestSessionParams): Future[String] = {
    val currentDate = new Date()
    val payload = mapper.writeValueAsString(transformEnqueueTestSessionParamstoPayload(params, customerConfig.source))

    val properties = PersistentMsgProperties(
      maybeAppId = Some(appInfo.name),
      maybeMessageId = Some(params.requestId),
      maybeExpiration = Some(ExpirationInMs),
      maybeType = Some(CreateTestSessionCmd.toString),
      maybeTimestamp = Some(currentDate),
      maybeCorrelationId = None,
      headers = Map.empty
    )

    val message = Message(payload, properties)

    queueConsumer
      .onConsume(message)
      .map {
        case true  => dbfsConfig.magicLinkPath + "/" + extractEpisodeRef(message)
        case false => "Message processing failed"
      }
      .rescue { case ex: Throwable =>
        info(s"Message processing encountered an exception: ${ex.getMessage}")
        Future.value("Default error message")
      }
  }

  private def extractEpisodeRef(message: Message): String = {
    mapper.parse[CreateTestSessionCommand](message.body).episodeRef
  }

  override def publishSuccess(taskContext: TaskContext, magicLinkUrl: String, params: PublishParams): Future[Unit] = {
    val payloadObj = transformPublishParamsIntoSuccessPayload(params, magicLinkUrl)
    val payload    = mapper.writeValueAsString(payloadObj)

    val properties = generateMsgProperties(appInfo, taskContext, NotifyClientTaskCmd)

    val publisher = ctx.createDefaultPublisher(config)
    val message   = Message(payload, properties)
    info(s"publishing to queue, success message  with requestId: ${params.requestId}")
    publisher.publish(message).ensure(publisher.close()).rescue { case e: Exception =>
      Future.exception(PublisherException(message, e.getMessage))
    }

  }

  override def publishFailure(taskContext: TaskContext, params: PublishParams): Future[Unit] = {
    val payloadObj = transformPublishParamsIntoFailurePayload(params)
    val payload    = mapper.writeValueAsString(payloadObj)

    val properties = generateMsgProperties(appInfo, taskContext, NotifyClientTaskCmd)

    val publisher = ctx.createDefaultPublisher(config)
    val message   = Message(payload, properties)

    info(s"publishing to queue, failure message  with requestId: ${params.requestId}")
    publisher.publish(message).ensure(publisher.close()).rescue { case e: Exception =>
      Future.exception(PublisherException(message, e.getMessage))
    }
  }

}

object SelfPublisherImpl {
  private final val ExpirationInMs = Duration.fromDays(7).inMilliseconds.toString

  private def transformEnqueueTestSessionParamstoPayload(params: EnqueueTestSessionParams,
                                                         source: String
                                                        ): EnqueueTestSessionMessagePayload =
    params
      .into[EnqueueTestSessionMessagePayload]
      .withFieldComputed(_.startDate, _ => LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
      .withFieldComputed(_.expiryDate, _ => LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.MINUTES))
      .withFieldConst(_.source, source)
      .transform

  private def transformPublishParamsIntoSuccessPayload(params: PublishParams,
                                                       magicLinkUrl: String
                                                      ): PublishSuccessPayload = {
    params
      .into[PublishSuccessPayload]
      .withFieldConst(_.outcome, 1)
      .withFieldConst(_.magicLinkUrl, magicLinkUrl)
      .transform
  }

  private def transformPublishParamsIntoFailurePayload(params: PublishParams): PublishFailurePayload = {
    params.into[PublishFailurePayload].withFieldConst(_.outcome, 0).transform
  }

  private def generateMsgProperties(appInfo: AppInfo, taskContext: TaskContext, cmd: Command): PersistentMsgProperties = {
    PersistentMsgProperties(
      maybeAppId = Some(appInfo.name),
      maybeMessageId = Some(taskContext.messageId),
      maybeExpiration = Some(ExpirationInMs),
      maybeType = Some(cmd.toString),
      maybeTimestamp = Some(new Date()),
      maybeCorrelationId = None,
      headers = Map.empty
    )
  }

  case class PublishParams(requestId: String, patientRef: String, episodeRef: String, locationId: String)

  case class PublishSuccessPayload(requestId: String,
                                   patientRef: String,
                                   episodeRef: String,
                                   locationId: String,
                                   outcome: Int,
                                   magicLinkUrl: String
                                  )

  case class PublishFailurePayload(requestId: String,
                                   patientRef: String,
                                   episodeRef: String,
                                   locationId: String,
                                   outcome: Int
                                  )

  final case class EnqueueTestSessionMessagePayload(
      requestId: String,
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

}

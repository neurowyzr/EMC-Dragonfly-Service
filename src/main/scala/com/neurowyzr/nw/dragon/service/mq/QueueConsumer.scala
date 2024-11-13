package com.neurowyzr.nw.dragon.service.mq

import javax.inject.Inject

import scala.jdk.CollectionConverters.MapHasAsJava

import com.twitter.finagle.context.Contexts
import com.twitter.util.{Future, Return, Throw, Try}
import com.twitter.util.jackson.ScalaObjectMapper
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.biz.{
  CreateMagicLinkTaskPipeline, CreateTestSessionTaskPipeline, CreateUserTaskPipeline, InvalidateMagicLinkTaskPipeline,
  NotifyClientTaskPipeline, UpdateMagicLinkTaskPipeline, UploadReportTaskPipeline
}
import com.neurowyzr.nw.dragon.service.biz.exceptions.{
  BizException, CreateTestSessionException, ErrorOutcomeException, PublisherException
}
import com.neurowyzr.nw.dragon.service.biz.models.*
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus.DuplicateEpisodeId
import com.neurowyzr.nw.dragon.service.cfg.Models.DbfsConfig
import com.neurowyzr.nw.dragon.service.mq.Models.{
  CreateMagicLinkCommand, CreateTestSessionCommand, CreateUserCommand, InvalidateMagicLinkCommand, NotifyClientCommand,
  UpdateMagicLinkCommand, UploadCommand
}
import com.neurowyzr.nw.dragon.service.mq.QueueConsumer.{createLoggingMarker, extractTaskContext}
import com.neurowyzr.nw.dragon.service.mq.QueueConsumer.Implicits.{
  CreateMagicLinkCommandToTaskInputTransformer, CreateTestSessionCommandToTaskInputTransformer,
  CreateUserCommandToTaskInputTransformer, InvalidateMagicLinkCommandToTaskInputTransformer,
  NotifyClientCommandToTaskInputTransformer, UpdateMagicLinkCommandToTaskInputTransformer
}
import com.neurowyzr.nw.dragon.service.mq.impl.CygnusPublisherImpl
import com.neurowyzr.nw.dragon.service.mq.impl.SelfPublisherImpl.PublishParams
import com.neurowyzr.nw.dragon.service.utils.context.MessageContext
import com.neurowyzr.nw.dragon.service.utils.context.MessageContext.MessageContextKey
import com.neurowyzr.nw.dragon.service as root
import com.neurowyzr.nw.finatra.lib.clients.AlerterHttpClient
import com.neurowyzr.nw.finatra.lib.pipeline.FExecutor
import com.neurowyzr.nw.finatra.rabbitmq.lib.{MqConsumer, MqConsumerHandler}
import com.neurowyzr.nw.finatra.rabbitmq.lib.Models.Message

import com.google.inject.Singleton
import io.scalaland.chimney.dsl.TransformationOps
import net.logstash.logback.marker.MapEntriesAppendingMarker

@Singleton
class QueueConsumer @Inject() (mapper: ScalaObjectMapper,
                               consumer: MqConsumer,
                               createTestSessionTaskPipeline: CreateTestSessionTaskPipeline,
                               createMagicLinkTaskPipeline: CreateMagicLinkTaskPipeline,
                               createUserTaskPipeline: CreateUserTaskPipeline,
                               updateMagicLinkTaskPipeline: UpdateMagicLinkTaskPipeline,
                               invalidateMagicLinkTaskPipeline: InvalidateMagicLinkTaskPipeline,
                               notifyClientTaskPipeline: NotifyClientTaskPipeline,
                               uploadReportTaskPipeline: UploadReportTaskPipeline,
                               cygnusPublisher: CygnusPublisherImpl,
                               selfPublisher: SelfPublisher,
                               dbfsConfig: DbfsConfig,
                               alerter: AlerterHttpClient
                              )
    extends MqConsumerHandler with Logging {

  init()

  override def onConsume(message: Message): Future[Boolean] = {
    val msgContext = MessageContext(message)
    warn(createLoggingMarker(msgContext), "Message processing started..: " + message.toString)

    Contexts.local.let(MessageContextKey, msgContext) {
      selectExecutor(message) match {
        case Return(executor) =>
          executor.execute.transform {
            case Return(_) => // message is accepted
              Future.True

            case Throw(ex: BizException) => // message is discarded
              warn(createLoggingMarker(msgContext), "Message processing failed: " + ex.getMessage, ex)
              Future.False

            case Throw(ex: ErrorOutcomeException) =>
              warn(
                createLoggingMarker(msgContext),
                s"Message failed to process due to error code: ${ex.outcome.getErrorCode} and error message: ${ex.errorMsg}"
              )
              extractTaskContext(message).flatMap { taskContext =>
                Try(cygnusPublisher.publishOutcome(taskContext, ex.outcome))
              }
              Future.False

            case Throw(ex: CreateTestSessionException) =>
              error(
                createLoggingMarker(msgContext),
                s"Message failed to process due to error: ${ex.status.toString} and error message: ${ex.errorMsg}",
                ex
              )

              Future
                .const(extractTaskContext(message))
                .flatMap { taskContext =>
                  ex.status match {
                    case DuplicateEpisodeId =>
                      selfPublisher
                        .publishSuccess(
                          taskContext,
                          dbfsConfig.magicLinkPath + "/" + extractEpisodeRef(message),
                          extractPublishParams(message)
                        )
                        .map(_ => true)
                    case _ => selfPublisher.publishFailure(taskContext, extractPublishParams(message)).map(_ => false)
                  }
                }
                .rescue { case e: PublisherException =>
                  error(createLoggingMarker(msgContext), "Failed to publish message: " + e.message.toString, e)
                  alerter.notify("[ERROR] Failed to publish message to dragonfly-apollo queue", e.message.toString)
                  Future.False
                }

            case Throw(ex) => // message is retried later
              Future.exception(ex)
          }

        case Throw(ex) => // message is discarded
          warn(createLoggingMarker(msgContext), "Message parsing failed:\n" + message.body, ex)
          Future.False
      }
    }
  }

  private def init(): Future[Unit] = {
    consumer.consume(this)
  }

  private def selectExecutor(message: Message): Try[FExecutor[Task]] = {
    extractTaskContext(message).flatMap { context =>
      implicit val pipeline1: CreateTestSessionTaskPipeline   = createTestSessionTaskPipeline
      implicit val pipeline2: CreateMagicLinkTaskPipeline     = createMagicLinkTaskPipeline
      implicit val pipeline3: CreateUserTaskPipeline          = createUserTaskPipeline
      implicit val pipeline4: UpdateMagicLinkTaskPipeline     = updateMagicLinkTaskPipeline
      implicit val pipeline5: InvalidateMagicLinkTaskPipeline = invalidateMagicLinkTaskPipeline
      implicit val pipeline6: NotifyClientTaskPipeline        = notifyClientTaskPipeline
      implicit val pipeline7: UploadReportTaskPipeline        = uploadReportTaskPipeline

      message.`type` match {
        case "UploadCmd" =>
          Try(mapper.parse[UploadCommand](message.body))
            .map(command => UploadReportTask(context, command.into[UploadReportTaskInput].transform))
            .map(new FExecutor(_))
        case "NotifyClientTaskCmd" =>
          Try(mapper.parse[NotifyClientCommand](message.body))
            .map(command => NotifyClientTask(context, NotifyClientCommandToTaskInputTransformer.transform(command)))
            .map(new FExecutor(_))
        case "CreateTestSessionCmd" =>
          Try(mapper.parse[CreateTestSessionCommand](message.body))
            .map(command =>
              CreateTestSessionTask(context, CreateTestSessionCommandToTaskInputTransformer.transform(command))
            )
            .map(new FExecutor(_))
        case "CreateMagicLinkCmd" =>
          Try(mapper.parse[CreateMagicLinkCommand](message.body))
            .map(command => CreateMagicLinkTask(context, CreateMagicLinkCommandToTaskInputTransformer.transform(command)))
            .map(new FExecutor(_))
        case "CreateUserCmd" =>
          Try(mapper.parse[CreateUserCommand](message.body))
            .map(command => CreateUserTask(context, CreateUserCommandToTaskInputTransformer.transform(command)))
            .map(new FExecutor(_))
        case "UpdateMagicLinkCmd" =>
          Try(mapper.parse[UpdateMagicLinkCommand](message.body))
            .map(command => UpdateMagicLinkTask(context, UpdateMagicLinkCommandToTaskInputTransformer.transform(command)))
            .map(new FExecutor(_))
        case "InvalidateMagicLinkCmd" =>
          Try(mapper.parse[InvalidateMagicLinkCommand](message.body))
            .map(command =>
              InvalidateMagicLinkTask(context, InvalidateMagicLinkCommandToTaskInputTransformer.transform(command))
            )
            .map(new FExecutor(_))
        case _ => Throw(BizException(s"The message of type `${message.`type`}` was not handled."))
      }
    }
  }

  private def extractEpisodeRef(message: Message): String = {
    mapper.parse[CreateTestSessionCommand](message.body).episodeRef
  }

  private def extractPublishParams(message: Message): PublishParams = {
    mapper.parse[CreateTestSessionCommand](message.body).into[PublishParams].transform
  }

}

object QueueConsumer {
  import io.scalaland.chimney.Transformer

  private[mq] def extractTaskContext(message: Message): Try[TaskContext] = {
    import Implicits.MsgPropertiesToTaskContextTransformer
    message.maybeProperties
      .map(properties => Try(MsgPropertiesToTaskContextTransformer.transform(properties)))
      .getOrElse(Throw[TaskContext](BizException("Message properties were not found.")))
  }

  private def createLoggingMarker(context: MessageContext) = new MapEntriesAppendingMarker(context.toMap.asJava)

  object Implicits {

    final implicit val NotifyClientCommandToTaskInputTransformer: Transformer[
      root.mq.Models.NotifyClientCommand,
      NotifyClientTaskInput
    ] =
      Transformer
        .define[
          com.neurowyzr.nw.dragon.service.mq.Models.NotifyClientCommand,
          NotifyClientTaskInput
        ]
        .buildTransformer

    final implicit val CreateTestSessionCommandToTaskInputTransformer: Transformer[
      root.mq.Models.CreateTestSessionCommand,
      CreateTestSessionTaskInput
    ] =
      Transformer
        .define[
          com.neurowyzr.nw.dragon.service.mq.Models.CreateTestSessionCommand,
          CreateTestSessionTaskInput
        ]
        .buildTransformer

    final implicit val CreateMagicLinkCommandToTaskInputTransformer: Transformer[
      root.mq.Models.CreateMagicLinkCommand,
      CreateMagicLinkTaskInput
    ] =
      Transformer
        .define[
          com.neurowyzr.nw.dragon.service.mq.Models.CreateMagicLinkCommand,
          CreateMagicLinkTaskInput
        ]
        .buildTransformer

    final implicit val CreateUserCommandToTaskInputTransformer: Transformer[
      root.mq.Models.CreateUserCommand,
      CreateUserTaskInput
    ] =
      Transformer
        .define[
          root.mq.Models.CreateUserCommand,
          CreateUserTaskInput
        ]
        .buildTransformer

    final implicit val UpdateMagicLinkCommandToTaskInputTransformer: Transformer[
      root.mq.Models.UpdateMagicLinkCommand,
      UpdateMagicLinkTaskInput
    ] =
      Transformer
        .define[
          com.neurowyzr.nw.dragon.service.mq.Models.UpdateMagicLinkCommand,
          UpdateMagicLinkTaskInput
        ]
        .buildTransformer

    final implicit val InvalidateMagicLinkCommandToTaskInputTransformer: Transformer[
      root.mq.Models.InvalidateMagicLinkCommand,
      InvalidateMagicLinkTaskInput
    ] =
      Transformer
        .define[
          com.neurowyzr.nw.dragon.service.mq.Models.InvalidateMagicLinkCommand,
          InvalidateMagicLinkTaskInput
        ]
        .buildTransformer

    final implicit val MsgPropertiesToTaskContextTransformer: Transformer[
      com.neurowyzr.nw.finatra.rabbitmq.lib.Models.MsgProperties,
      TaskContext
    ] =
      Transformer
        .define[
          com.neurowyzr.nw.finatra.rabbitmq.lib.Models.MsgProperties,
          TaskContext
        ]
        .enableMethodAccessors
        .buildTransformer

  }

}

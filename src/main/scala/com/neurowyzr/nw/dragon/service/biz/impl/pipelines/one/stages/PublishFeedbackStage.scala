package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import javax.inject.Inject

import scala.jdk.CollectionConverters.MapHasAsJava

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.{CreateTestSessionException, PublisherException}
import com.neurowyzr.nw.dragon.service.biz.models.{CreateTestSessionTask, TaskContext}
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus.PublishFeedbackFailure
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.Success
import com.neurowyzr.nw.dragon.service.cfg.Models.AlerterServiceConfig
import com.neurowyzr.nw.dragon.service.mq.SelfPublisher
import com.neurowyzr.nw.dragon.service.mq.impl.SelfPublisherImpl.PublishParams
import com.neurowyzr.nw.dragon.service.utils.context.MessageContext
import com.neurowyzr.nw.finatra.lib.clients.AlerterHttpClient
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import io.scalaland.chimney.dsl.TransformationOps
import net.logstash.logback.marker.MapEntriesAppendingMarker

private[impl] class PublishFeedbackStage @Inject() (selfPublisher: SelfPublisher, alerter: AlerterHttpClient)
    extends FStage[CreateTestSessionTask] {

  override def execute(task: CreateTestSessionTask): Future[CreateTestSessionTask] = {
    info("Publish was called")
    Future.value(task)
//    task.out.maybeOutcome match {
//      case Some(Success) =>
//        selfPublisher
//          .publishSuccess(
//            task.ctx,
//            task.out.maybeMagicLinkUrl.get,
//            task.in.into[PublishParams].transform
//          )
//          .flatMap(_ => Future.value(task))
//          .rescue { case e: PublisherException =>
//            val message = e.message
//            error(createLoggingMarker(MessageContext(message)), "Failed to publish message: " + message.toString, e)
//            alerter.notify("[ERROR] Failed to publish message to dragonfly-apollo queue", message.toString)
//            Future.value(task)
//          }
//      case _ =>
//        val reason = s"Unexpected outcome: ${task.out.maybeOutcome.toString} and request id: ${task.in.requestId}."
//        warn(reason)
//        abort(CreateTestSessionException(PublishFeedbackFailure, reason))
//    }
  }

  private def createLoggingMarker(context: MessageContext) = new MapEntriesAppendingMarker(context.toMap.asJava)

}

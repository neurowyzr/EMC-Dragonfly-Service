package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.two.stages

import javax.inject.Inject

import scala.jdk.CollectionConverters.MapHasAsJava

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.{CreateTestSessionException, PublisherException}
import com.neurowyzr.nw.dragon.service.biz.models.{CreateTestSessionTask, NotifyClientTask, NotifyClientTaskInput}
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus.PublishFeedbackFailure
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.Success
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.CreateTestSessionArgs
import com.neurowyzr.nw.dragon.service.clients.CustomerHttpClient
import com.neurowyzr.nw.dragon.service.utils.context.MessageContext
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.TransformationOps
import net.logstash.logback.marker.MapEntriesAppendingMarker

private[impl] class NotifyClientStage @Inject() (customerHttpClient: CustomerHttpClient)
    extends FStage[NotifyClientTask] {

  override def execute(task: NotifyClientTask): Future[NotifyClientTask] = {
    info("notify is being called")
    task.in.maybeMagicLink match {
      case Some(magicLink) =>
        customerHttpClient.notifyCreateSucceeded(task.in.into[CreateTestSessionArgs].transform, magicLink)
      case _ => customerHttpClient.notifyCreateFailed(task.in.into[CreateTestSessionArgs].transform, 0)
    }

    Future.value(task)
  }

}

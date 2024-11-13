package com.neurowyzr.nw.dragon.service

import java.util.concurrent.TimeUnit

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Duration, Future}
import com.twitter.util.jackson.ScalaObjectMapper
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.SharedFakes.FakeUserBatchCode
import com.neurowyzr.nw.dragon.service.SmokeTest.*
import com.neurowyzr.nw.dragon.service.cfg.Models.AllConfig
import com.neurowyzr.nw.dragon.service.clients.impl.TestHttpClientConfigurator
import com.neurowyzr.nw.dragon.service.data.impl.Fakes.*
import com.neurowyzr.nw.dragon.service.data.impl.TestDao
import com.neurowyzr.nw.finatra.lib.clients.HttpClientConfigurator
import com.neurowyzr.nw.finatra.rabbitmq.lib.{MqConsumer, MqContext, MqPublisher}
import com.neurowyzr.nw.finatra.rabbitmq.lib.cfg.Models.*
import com.neurowyzr.nw.finatra.rabbitmq.lib.cfg.Models.Args.queueType

import com.google.inject.Stage

trait SmokeTest extends FeatureTest with Logging {

  protected lazy val testDao = new TestDao

  private lazy val testServer: EmbeddedHttpServer = createTestServer
    .bind[HttpClientConfigurator]
    .to(classOf[TestHttpClientConfigurator])

  protected lazy val mapper: ScalaObjectMapper = server.injector.instance[ScalaObjectMapper]
  protected lazy val mqContext: MqContext      = server.injector.instance[MqContext]

  private lazy val allConfig                              = testServer.injector.instance[AllConfig]
  private lazy val dragonPublisherConfig: PublisherConfig = allConfig.mq.selfPublisher

  def createPublisher(label: String): MqPublisher & AutoCloseable = mqContext.createDefaultPublisher(
    PublisherConfig(
      s"dragon-publisher-$label",
      isEnabled = true,
      dragonPublisherConfig.exchangeName,
      "",
      isMandatory = true,
      mustWaitForConfirm = true
    )
  )

  def createConsumer(label: String): MqConsumer & AutoCloseable = mqContext.createDefaultConsumer(
    ConsumerConfig(
      s"cygnus-consumer-$label",
      isEnabled = true,
      CygnusQueueName,
      DefaultPrefetchCount,
      isRequeuedToOriginal = false,
      DefaultTimeout
    )
  )

  protected lazy val consumedMessages = new java.util.concurrent.ConcurrentLinkedQueue[String]

  protected def createTestServer =
    new EmbeddedHttpServer(
      twitterServer = new ServerApp,
      stage = Stage.PRODUCTION,
      disableTestLogging = true
    )

  override protected def server: EmbeddedHttpServer = testServer

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    Thread.sleep(500)
    testDao.clearAll()
    setup()
  }

  override protected def afterAll(): Unit = {
    testDao.close()
    super.afterAll()
  }

  protected def prettifyJson(json: String): String = {
    mapper.writePrettyString(json)
  }

  private def setup(): Future[Unit] = {
    val newClientId  = testDao.insertNewClient(FakeClient)
    val newProductId = testDao.insertNewProduct(FakeProduct)
    val newEngagementId = testDao.insertNewEngagement(
      FakeEngagement.copy(clientId = newClientId, productId = newProductId)
    )
    val newUserBatchId = testDao.insertNewUserBatch(
      FakeUserBatch.copy(engagementId = newEngagementId, maybeCode = Some(FakeUserBatchCode))
    )
    val newUserId = testDao.insertNewUser(FakeUser)
    val _         = setupTopology()
    Future.Unit
  }

  private def setupTopology(): Future[Unit] = {
    val cygnusExchangeConfig = ExchangeConfig(CygnusExchangeName, "direct")
    val cygnusQueueConfig    = QueueConfig(CygnusQueueName, queueType("classic"))
    val cygnusBinding        = BindingConfig(cygnusExchangeConfig, cygnusQueueConfig)

    val mqManager = mqContext.createDefaultManager()
    mqManager
      .createTopology(
        TopologyConfig(
          Seq(cygnusExchangeConfig),
          Seq(cygnusQueueConfig),
          Seq(cygnusBinding)
        )
      )
      .ensure(mqManager.close())
  }

}

protected object SmokeTest {
  final val DefaultTimeout             = Duration(5, TimeUnit.SECONDS)
  final val CygnusExchangeName: String = "med-cygnus-x-test"
  final val CygnusQueueName: String    = "med-cygnus-q-test"
}

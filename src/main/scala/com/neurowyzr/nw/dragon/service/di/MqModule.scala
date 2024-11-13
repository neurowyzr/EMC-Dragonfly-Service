package com.neurowyzr.nw.dragon.service.di

import javax.inject.Singleton

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.Await
import com.twitter.util.jackson.ScalaObjectMapper

import com.neurowyzr.nw.dragon.service.biz.{
  CreateMagicLinkTaskPipeline, CreateTestSessionTaskPipeline, CreateUserTaskPipeline, InvalidateMagicLinkTaskPipeline,
  NotifyClientTaskPipeline, UpdateMagicLinkTaskPipeline, UploadReportTaskPipeline
}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.{
  CreateMagicLinkTaskPipelineImpl, CreateUserTaskPipelineImpl, InvalidateMagicLinkTaskPipelineImpl,
  UpdateMagicLinkTaskPipelineImpl
}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.CreateTestSessionTaskPipelineImpl
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.three.UploadReportTaskPipelineImpl
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.two.NotifyClientTaskPipelineImpl
import com.neurowyzr.nw.dragon.service.cfg.Models.{CustomerConfig, DbfsConfig}
import com.neurowyzr.nw.dragon.service.clients.{AwsS3Client, CoreHttpClient, CustomerHttpClient}
import com.neurowyzr.nw.dragon.service.data.*
import com.neurowyzr.nw.dragon.service.di.annotations.{EmailPublisherBinding, SelfPublisherBinding}
import com.neurowyzr.nw.dragon.service.mq.{EmailPublisher, QueueConsumer, SelfPublisher}
import com.neurowyzr.nw.dragon.service.mq.impl.{CygnusPublisherImpl, EmailPublisherImpl, SelfPublisherImpl}
import com.neurowyzr.nw.finatra.lib.cfg.Models.AppInfo
import com.neurowyzr.nw.finatra.lib.clients.AlerterHttpClient
import com.neurowyzr.nw.finatra.rabbitmq.lib.{MqClient, MqConsumer, MqContext}
import com.neurowyzr.nw.finatra.rabbitmq.lib.cfg.Models.{BrokerConfig, ConsumerConfig, PublisherConfig, TopologyConfig}
import com.neurowyzr.nw.finatra.rabbitmq.lib.impl.MqContextImpl

import com.google.inject.{Module, Provides}

object MqModule extends TwitterModule {

  private val MqClients = scala.collection.mutable.ArrayDeque.empty[MqClient]

  override def modules: Seq[Module] = Seq(
    ConfigModule
  )

  override def singletonStartup(injector: Injector): Unit = {
    val topology = injector.instance(classOf[TopologyConfig])
    val ctx      = injector.instance(classOf[MqContext])
    val manager  = ctx.createDefaultManager()

    try Await.result(manager.createTopology(topology), 5.second)
    finally manager.close()
  }

  override def singletonPostWarmupComplete(injector: Injector): Unit = {
    // this will force the consumers to be created
    val _ = injector.instance(classOf[QueueConsumer])
  }

  @Provides
  @Singleton
  def providesMqContext(config: BrokerConfig): MqContext = {
    val rmqExecutor = MqContext.createManagedExecutor("rmq")
    val context     = new MqContextImpl(config, rmqExecutor)

    onExit {
      val clients = MqClients.reverse.toSeq
      MqClients.clear()

      context.closeClients(clients)
      context.close()
    }

    context
  }

  @Provides
  @Singleton
  def providesMqConsumer(ctx: MqContext, config: ConsumerConfig): MqConsumer = {
    val consumer = ctx.createDefaultConsumer(config)
    MqClients += consumer
    consumer
  }

  @Provides
  @Singleton
  def providesUploadReportTaskPipeline(episodeRepository: EpisodeRepository,
                                       userRepository: UserRepository,
                                       userBatchLookupRepository: UserBatchLookupRepository,
                                       customerHttpClient: CustomerHttpClient,
                                       awsS3Client: AwsS3Client
                                      ): UploadReportTaskPipeline = {
    new UploadReportTaskPipelineImpl(
      episodeRepository,
      userRepository,
      userBatchLookupRepository,
      customerHttpClient,
      awsS3Client
    )
  }

  @Provides
  @Singleton
  def providesNotifyClientTaskPipeline(customerHttpClient: CustomerHttpClient): NotifyClientTaskPipeline = {
    new NotifyClientTaskPipelineImpl(customerHttpClient)
  }

  @Provides
  @Singleton
  def providesCreateTestSessionTaskPipeline(episodeRepository: EpisodeRepository,
                                            userBatchLookupRepository: UserBatchLookupRepository,
                                            userBatchRepository: UserBatchRepository,
                                            engagementRepository: EngagementRepository,
                                            userRepository: UserRepository,
                                            userAccountRepository: UserAccountRepository,
                                            userAccountAudRepository: UserAccountAudRepository,
                                            userRoleRepository: UserRoleRepository,
                                            revInfoRepository: RevInfoRepository,
                                            dbfsConfig: DbfsConfig,
                                            customerConfig: CustomerConfig,
                                            coreHttpClient: CoreHttpClient,
                                            alerterHttpClient: AlerterHttpClient,
                                            selfPublisher: SelfPublisher
                                           ): CreateTestSessionTaskPipeline = {
    new CreateTestSessionTaskPipelineImpl(
      episodeRepository,
      userBatchLookupRepository,
      userBatchRepository,
      engagementRepository,
      userRepository,
      userAccountRepository,
      userAccountAudRepository,
      userRoleRepository,
      revInfoRepository,
      dbfsConfig,
      customerConfig,
      coreHttpClient,
      alerterHttpClient,
      selfPublisher
    )
  }

  @Provides
  @Singleton
  def providesCreateMagicLinkTaskPipeline(episodeRepository: EpisodeRepository,
                                          userRepository: UserRepository,
                                          userBatchRepository: UserBatchRepository,
                                          engagementRepository: EngagementRepository,
                                          userAccountRepository: UserAccountRepository,
                                          cygnusRepo: CygnusEventRepository,
                                          producer: CygnusPublisherImpl
                                         ): CreateMagicLinkTaskPipeline = {
    new CreateMagicLinkTaskPipelineImpl(episodeRepository,
                                        userRepository,
                                        userBatchRepository,
                                        engagementRepository,
                                        userAccountRepository,
                                        cygnusRepo,
                                        producer
                                       )
  }

  @Provides
  @Singleton
  def providesCreateUserTaskPipeline(userRepository: UserRepository,
                                     cygnusRepo: CygnusEventRepository,
                                     dbfsConfig: DbfsConfig,
                                     producer: CygnusPublisherImpl
                                    ): CreateUserTaskPipeline = {
    new CreateUserTaskPipelineImpl(userRepository, cygnusRepo, dbfsConfig, producer)
  }

  @Provides
  @Singleton
  def providesUpdateMagicLinkTaskPipeline(episodeRepository: EpisodeRepository,
                                          cygnusRepo: CygnusEventRepository,
                                          producer: CygnusPublisherImpl
                                         ): UpdateMagicLinkTaskPipeline = {
    new UpdateMagicLinkTaskPipelineImpl(episodeRepository, cygnusRepo, producer)
  }

  @Provides
  @Singleton
  def providesInvalidateMagicLinkTaskPipeline(episodeRepository: EpisodeRepository,
                                              cygnusRepo: CygnusEventRepository,
                                              producer: CygnusPublisherImpl
                                             ): InvalidateMagicLinkTaskPipeline = {
    new InvalidateMagicLinkTaskPipelineImpl(episodeRepository, cygnusRepo, producer)
  }

  @Provides
  @Singleton
  def providesEmailPublisher(mapper: ScalaObjectMapper,
                             ctx: MqContext,
                             @EmailPublisherBinding config: PublisherConfig,
                             appInfo: AppInfo
                            ): EmailPublisher = {
    new EmailPublisherImpl(mapper, ctx, config, appInfo)
  }

  @Provides
  @Singleton
  def providesSelfPublisher(mapper: ScalaObjectMapper,
                            ctx: MqContext,
                            @SelfPublisherBinding config: PublisherConfig,
                            appInfo: AppInfo,
                            customerConfig: CustomerConfig,
                            queueConsumer: QueueConsumer,
                            dbfsConfig: DbfsConfig
                           ): SelfPublisher = {
    new SelfPublisherImpl(mapper, ctx, config, appInfo, customerConfig, queueConsumer, dbfsConfig)
  }

}

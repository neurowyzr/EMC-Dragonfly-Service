package com.neurowyzr.nw.dragon.service.di

import javax.inject.Singleton

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.jackson.ScalaObjectMapper

import com.neurowyzr.nw.dragon.service.cfg.Models.{
  AlerterServiceConfig, AwsConfig, CoreServiceConfig, CustomerConfig, CustomerServiceConfig
}
import com.neurowyzr.nw.dragon.service.clients.{AwsS3Client, CoreHttpClient, CustomerHttpClient}
import com.neurowyzr.nw.dragon.service.clients.impl.{AwsS3ClientImpl, CoreHttpClientImpl, CustomerHttpClientImpl}
import com.neurowyzr.nw.finatra.lib.clients.{AlerterHttpClient, HttpClientConfigurator}
import com.neurowyzr.nw.finatra.lib.clients.impl.{
  AlerterHttpClientForGoogleChat, DefaultHttpClientConfigurator, DefaultHttpClientProvider
}

import com.google.inject.{Module, Provides}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

object ClientModule extends TwitterModule {

  override def modules: Seq[Module] = Seq(
    ConfigModule
  )

  @Provides
  @Singleton
  def providesHttpClientConfigurator(): HttpClientConfigurator = {
    new DefaultHttpClientConfigurator()
  }

  @Provides
  @Singleton
  def providesDbfsClient(config: CoreServiceConfig,
                         clientConfigurator: HttpClientConfigurator,
                         injector: Injector,
                         statsReceiver: StatsReceiver,
                         mapper: ScalaObjectMapper
                        ): CoreHttpClient = {
    val client = createHttpClient(config.source,
                                  config.destination,
                                  config.address,
                                  clientConfigurator,
                                  injector,
                                  statsReceiver,
                                  mapper
                                 )
    new CoreHttpClientImpl(client)
  }

  private def createHttpClient(source: String,
                               target: String,
                               endpointPath: String,
                               clientConfigurator: HttpClientConfigurator,
                               injector: Injector,
                               statsReceiver: StatsReceiver,
                               mapper: ScalaObjectMapper
                              ): HttpClient = {
    val provider = {
      new DefaultHttpClientProvider(source, target, endpointPath, clientConfigurator, injector, statsReceiver, mapper)
    }
    provider.provideHttpClient()
  }

  @Provides
  @Singleton
  def providesAlerterClient(config: AlerterServiceConfig,
                            clientConfigurator: HttpClientConfigurator,
                            injector: Injector,
                            statsReceiver: StatsReceiver,
                            mapper: ScalaObjectMapper
                           ): AlerterHttpClient = {
    val client = createHttpClient(config.source,
                                  config.destination,
                                  config.address,
                                  clientConfigurator,
                                  injector,
                                  statsReceiver,
                                  mapper
                                 )
    new AlerterHttpClientForGoogleChat(client, mapper, config.args)
  }

  @Provides
  @Singleton
  def providesCustomerClient(config: CustomerServiceConfig,
                             clientConfigurator: HttpClientConfigurator,
                             injector: Injector,
                             statsReceiver: StatsReceiver,
                             mapper: ScalaObjectMapper,
                             alerterHttpClient: AlerterHttpClient
                            ): CustomerHttpClient = {
    val client = createHttpClient(config.source,
                                  config.destination,
                                  config.address,
                                  clientConfigurator,
                                  injector,
                                  statsReceiver,
                                  mapper
                                 )
    new CustomerHttpClientImpl(config, client, alerterHttpClient)
  }

  @Provides
  @Singleton
  def providesS3Client(awsConfig: AwsConfig): S3Client = {
    val credentials = StaticCredentialsProvider.create(
      AwsBasicCredentials.create(awsConfig.accessKey, awsConfig.accessSecret.value)
    )

    val client = S3Client.builder().region(Region.of(awsConfig.region)).credentialsProvider(credentials).build()

    onExit {
      info("Closing s3 client")
      client.close()
    }

    client
  }

  @Provides
  @Singleton
  def providesAwsS3Client(s3Client: S3Client): AwsS3Client = {
    new AwsS3ClientImpl(s3Client)
  }

}

package com.neurowyzr.nw.dragon.service.di

import javax.inject.Singleton

import com.twitter.finatra.utils.FuturePools
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.{ExecutorServiceFuturePool, FuturePool}

import com.neurowyzr.nw.dragon.service.cfg.Models.DatabaseConfig
import com.neurowyzr.nw.dragon.service.data.{CoreSqlDbContext, DragonSqlDbContext}
import com.neurowyzr.nw.dragon.service.di.annotations.{
  CoreDatabaseBinding, CoreDatabaseContext, DragonDatabaseBinding, DragonflyDatabaseContext
}

import com.google.inject.{Module, Provides}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.SnakeCase

object DatabaseContextModule extends TwitterModule {

  private final val Name: String                    = "database-pool"
  private final val Size: Int                       = 10
  private final val Pool: ExecutorServiceFuturePool = FuturePools.fixedPool(Name, Size)

  override val modules: Seq[Module] = Seq(ConfigModule)

  @Provides
  @Singleton
  def providesFuturePool: FuturePool = Pool

  @Provides
  @Singleton
  @CoreDatabaseContext
  def providesCoreDatabaseContext(@CoreDatabaseBinding config: DatabaseConfig): CoreSqlDbContext = {
    val hikariConfig: HikariConfig = {
      val hikariConfig = new HikariConfig()
      hikariConfig.setDataSourceClassName(config.source)
      hikariConfig.setConnectionTimeout(7000)
      hikariConfig.addDataSourceProperty("serverName", config.host)
      hikariConfig.addDataSourceProperty("portNumber", config.port.toString)
      hikariConfig.addDataSourceProperty("databaseName", config.name)
      hikariConfig.addDataSourceProperty("user", config.username)
      hikariConfig.addDataSourceProperty("password", config.password)
      hikariConfig.setMaximumPoolSize(5)
      hikariConfig.setMinimumIdle(1)
      hikariConfig
    }

    val dataSource: HikariDataSource = new HikariDataSource(hikariConfig)

    val ctx = new CoreSqlDbContext(SnakeCase, dataSource)

    onExit {
      info("Shutting down database context.")
      ctx.close()
      dataSource.close()
    }

    ctx
  }

  @Provides
  @Singleton
  @DragonflyDatabaseContext
  def providesDragonflyDatabaseContext(@DragonDatabaseBinding config: DatabaseConfig): DragonSqlDbContext = {
    val hikariConfig: HikariConfig = {
      val hikariConfig = new HikariConfig()
      hikariConfig.setDataSourceClassName(config.source)
      hikariConfig.setConnectionTimeout(7000)
      hikariConfig.addDataSourceProperty("serverName", config.host)
      hikariConfig.addDataSourceProperty("portNumber", config.port.toString)
      hikariConfig.addDataSourceProperty("databaseName", config.name)
      hikariConfig.addDataSourceProperty("user", config.username)
      hikariConfig.addDataSourceProperty("password", config.password)
      hikariConfig.setMaximumPoolSize(5)
      hikariConfig.setMinimumIdle(1)
      hikariConfig
    }

    val dataSource: HikariDataSource = new HikariDataSource(hikariConfig)

    val ctx = new DragonSqlDbContext(SnakeCase, dataSource)

    onExit {
      info("Shutting down database context.")
      ctx.close()
      dataSource.close()
    }

    ctx
  }

  override def singletonShutdown(injector: Injector): Unit = {
    info("Shutting down future pool: " + Name)
    Pool.executor.shutdown()
  }

}

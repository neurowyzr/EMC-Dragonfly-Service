package com.neurowyzr.nw.dragon.service.di

import scala.jdk.CollectionConverters.ListHasAsScala

import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.cfg.Models.DatabaseConfig
import com.neurowyzr.nw.dragon.service.di.annotations.DragonDatabaseBinding

import com.google.inject.Module
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.flywaydb.core.api.output.MigrateResult

object MigrationModule extends TwitterModule {

  override def modules: Seq[Module] = Seq(
    ConfigModule,
    DataModule
  )

  override def singletonStartup(injector: Injector): Unit = {
    val config = injector.instance(classOf[DatabaseConfig], classOf[DragonDatabaseBinding])

    val flywayConfig = new ClassicConfiguration()
    val url          = s"jdbc:mysql://${config.host}:${config.port.toString}/${config.name}"

    flywayConfig.setDataSource(url, config.username, config.password)

    migrate(new Flyway(flywayConfig)): Unit
  }

  private[di] def migrate(flyway: Flyway): Try[Unit] = {
    Try(flyway.migrate)
      .onSuccess { (result: MigrateResult) =>
        if (result.success) {
          logger.info(s"Migration has succeeded with ${result.migrationsExecuted.toString} migrations.")
        } else {
          logger.warn("Migration has failed, reasons are: " + result.warnings.asScala.mkString(", "))
        }
      }
      .onFailure { throwable =>
        logger.error("Migration has errored, check the setting or connectivity or flyway history!", throwable)
      }
      .map(_ => {})
  }

}

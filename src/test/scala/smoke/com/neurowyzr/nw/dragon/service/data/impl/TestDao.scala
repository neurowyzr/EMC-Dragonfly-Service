package com.neurowyzr.nw.dragon.service.data.impl

import com.neurowyzr.nw.dragon.service.cfg.Models.DatabaseConfig
import com.neurowyzr.nw.dragon.service.data.CoreSqlDbContext
import com.neurowyzr.nw.dragon.service.data.impl.TestDao.TestDatabaseConfig
import com.neurowyzr.nw.dragon.service.data.models.*

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.SnakeCase

final class TestDao extends AutoCloseable {

  private[this] def createTestDataSource(config: DatabaseConfig): HikariDataSource = {
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

    new HikariDataSource(hikariConfig)
  }

  private final val testDataSource: HikariDataSource = createTestDataSource(TestDatabaseConfig)

  private final val ctx = new CoreSqlDbContext(SnakeCase, testDataSource)

  import ctx.*
  override def close(): Unit = ctx.close()

  protected val schema = new Schema(ctx)
  protected val query  = new Queries(ctx, schema)

  ctx.quote(schema.episode.delete)

  def clearAll(): Unit = {
    deleteAllEpisodes()
    deleteAllTestSessions()
    deleteAllUserAccounts()
    deleteAllUsers()
    deleteAllUserBatches()
    deleteAllEngagements()
    deleteAllClients()
    deleteAllProducts()
    deleteAllCygnusEvents()
  }

  def deleteAllEpisodes(): Unit = {
    val q = quote(schema.episode.delete)
    val _ = run(q)
  }

  def deleteAllTestSessions(): Unit = {
    val q = quote(schema.testSession.delete)
    val _ = run(q)
  }

  def deleteAllUsers(): Unit = {
    val q = quote(schema.users.delete)
    val _ = run(q)
  }

  def deleteAllUserBatches(): Unit = {
    val q = quote(schema.userBatch.delete)
    val _ = run(q)
  }

  def deleteAllEngagements(): Unit = {
    val q = quote(schema.engagements.delete)
    val _ = run(q)
  }

  def deleteAllClients(): Unit = {
    val q = quote(schema.clients.delete)
    val _ = run(q)
  }

  def deleteAllProducts(): Unit = {
    val q = quote(schema.products.delete)
    val _ = run(q)
  }

  def deleteAllUserAccounts(): Unit = {
    val q = quote(schema.userAccount.delete)
    val _ = run(q)
  }

  def deleteAllCygnusEvents(): Unit = {
    val q = quote(schema.cygnusEvent.delete)
    val _ = run(q)
  }

  def insertNewUser(entity: User): Long = run(query.insertNewUser(entity))

  def insertNewUserBatch(entity: UserBatch): Long = run(query.insertNewUserBatch(entity))

  def insertNewEngagement(entity: Engagement): Long = run(query.insertNewEngagement(entity))

  def insertNewClient(entity: Client): Long = run(query.insertNewClient(entity))

  def insertNewProduct(entity: Product): Long = run(query.insertNewProduct(entity))

  def getEpisodeByTestId(testId: String): Option[Episode] = run(query.getEpisodeByTestId(testId)).headOption

  def getUserByPatientRef(reference: String): Option[User] = run(query.getUserByExtPatientRef(reference)).headOption

  def allTestSessions(): Seq[TestSession] = run(query.allTestSessions)

  def allUserAccounts(): Seq[UserAccount] = run(query.allUserAccounts)
}

private object TestDao {

  final val TestDatabaseConfig = DatabaseConfig("com.mysql.cj.jdbc.MysqlDataSource",
                                                "cognifyx-core-test",
                                                "localhost",
                                                3306,
                                                "root",
                                                "password"
                                               )

  final val InitialisationTimeInMs: Long = 500
}

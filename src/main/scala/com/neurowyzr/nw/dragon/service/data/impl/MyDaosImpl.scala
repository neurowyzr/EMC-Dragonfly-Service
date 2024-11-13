package com.neurowyzr.nw.dragon.service.data.impl

import javax.inject.{Inject, Singleton}

import com.twitter.util.Try
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.data.{DragonSqlDbContext, MyDaos}
import com.neurowyzr.nw.dragon.service.data.impl.CoreDaosImpl.{logInsert, logQueryOpt, logUpdate}
import com.neurowyzr.nw.dragon.service.data.models.*
import com.neurowyzr.nw.dragon.service.data.models.Types.LongId

@SuppressWarnings(
  Array("org.wartremover.warts.StringPlusAny", "org.wartremover.warts.Serializable", "org.wartremover.warts.Product")
)
@Singleton
class MyDaosImpl @Inject() (ctx: DragonSqlDbContext) extends MyDaos with Logging {

  import ctx.*

  protected val schemaExtra = new SchemaExtra(ctx)
  protected val query       = new QueriesExtra(ctx, schemaExtra)

  // SampleDao
  override def insertSample(entity: Sample): Try[LongId] = {
    val tried: Try[LongId] = Try(ctx.run(query.insertSample(entity)))
    logInsert[LongId](tried, "product", (id: LongId) => id.toString)
  }

  override def insertSurveySelections(entity: UserSurvey): Try[String] = {
    val tried: Try[String] = Try(ctx.run(query.insertSurvey(entity)).toString)
    logInsert[String](tried, "user_surveys", _ => entity.sessionId)
  }

  override def insertConsent(entity: UserDataConsent): Try[String] = {
    val tried: Try[String] = Try(ctx.run(query.insertUserDataConsent(entity)).toString)
    logInsert[String](tried, "user_data_consent", _ => entity.userId.toString)
  }

  override def revokeConsentByUserId(userId: Long): Try[String] = {
    val tried: Try[String] = Try(ctx.run(query.revokeConsentByUserId(userId)).toString)
    logUpdate[String](tried, "user_data_consent", (id: String) => id)
  }

  override def getUserConsentByUserId(userId: Long): Try[Option[UserDataConsent]] = {
    val tried = Try(ctx.run(query.getUserConsentByUserId(userId)).headOption)
    logQueryOpt[UserDataConsent](tried, s"user consent by user id: $userId")
  }

  // UserBatchLookupDao
  override def getUserBatchLookupByKey(key: String): Try[Option[UserBatchLookup]] = {
    val tried = Try(ctx.run(query.getUserBatchLookupByKey(key)).headOption)
    logQueryOpt[UserBatchLookup](tried, s"user batch lookup by key: $key")
  }

  override def getUserBatchLookupByCode(userBatchCode: String): Try[Option[UserBatchLookup]] = {
    val tried = Try(ctx.run(query.getUserBatchLookupByCode(userBatchCode)).headOption)
    logQueryOpt[UserBatchLookup](tried, s"user batch lookup by code: $userBatchCode")
  }

}

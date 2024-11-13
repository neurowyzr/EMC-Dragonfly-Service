package com.neurowyzr.nw.dragon.service.data.impl

import java.time.{LocalDateTime, ZoneOffset}

import com.neurowyzr.nw.dragon.service.data.DragonSqlDbContext

import io.getquill.*

private[impl] class QueriesExtra(ctx: DragonSqlDbContext, schema: SchemaExtra) {

  import com.neurowyzr.nw.dragon.service.data.models.*

  import ctx.*

  // Episode
  private[impl] def insertSample(entity: Sample): Quoted[ActionReturning[Sample, Long]] = quote {
    schema.sample.insertValue(lift(entity)).returningGenerated(_.id)
  }

  private[impl] def insertSurvey(entity: UserSurvey): Quoted[Insert[UserSurvey]] = quote {
    schema.userSurvey.insertValue(lift(entity))
  }

  private[impl] def insertUserDataConsent(entity: UserDataConsent): Quoted[Insert[UserDataConsent]] = quote {
    schema.userDataConsent.insertValue(lift(entity))
  }

  private[impl] def revokeConsentByUserId(userId: Long): Quoted[Update[UserDataConsent]] = quote {
    schema.userDataConsent
      .filter(e => e.userId == lift(userId))
      .update(_.isConsent -> lift(false), _.maybeUtcRevokedAt -> Some(lift(LocalDateTime.now(ZoneOffset.UTC))))
  }

  private[impl] def getUserConsentByUserId(userId: Long): Quoted[Query[UserDataConsent]] = quote {
    schema.userDataConsent.filter(e => e.userId == lift(userId)).take(1)
  }

  // User Batch Lookup
  private[impl] def insertNewUserBatchLookup(entity: UserBatchLookup): Quoted[Insert[UserBatchLookup]] = quote {
    schema.userBatchLookup.insertValue(lift(entity))
  }

  private[impl] def getUserBatchLookupByKey(key: String): Quoted[Query[UserBatchLookup]] = quote {
    schema.userBatchLookup.filter(_.key == (lift(key))).take(1)
  }

  private[impl] def getUserBatchLookupByCode(userBatchCode: String): Quoted[Query[UserBatchLookup]] = quote {
    schema.userBatchLookup.filter(_.value == (lift(userBatchCode))).take(1)
  }

  // Non exposed methods meant for unit testing
  private[impl] def allUserSurvey: Quoted[EntityQuery[UserSurvey]] = quote {
    schema.userSurvey
  }

  private[impl] def allUserDataConsent: Quoted[EntityQuery[UserDataConsent]] = quote {
    schema.userDataConsent
  }

  private[impl] def allUserBatchLookup: Quoted[EntityQuery[UserBatchLookup]] = quote {
    schema.userBatchLookup
  }

}

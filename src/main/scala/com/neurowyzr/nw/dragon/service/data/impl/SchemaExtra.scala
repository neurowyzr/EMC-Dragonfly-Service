package com.neurowyzr.nw.dragon.service.data.impl

import com.neurowyzr.nw.dragon.service.data.DragonSqlDbContext

import io.getquill.{EntityQuery, Quoted}

private[impl] class SchemaExtra(ctx: DragonSqlDbContext) {

  import com.neurowyzr.nw.dragon.service.data.models.*

  import ctx.*

  private[impl] final val sample: Quoted[EntityQuery[Sample]] = quote {
    querySchema[Sample](
      entity = "sample"
    )
  }

  private[impl] final val userDataConsent: Quoted[EntityQuery[UserDataConsent]] = quote {
    querySchema[UserDataConsent](
      entity = "USER_DATA_CONSENT",
      _.maybeUtcRevokedAt -> "utc_revoked_at"
    )
  }

  private[impl] final val userSurvey: Quoted[EntityQuery[UserSurvey]] = quote {
    querySchema[UserSurvey](entity = "USER_SURVEYS")
  }

  private[impl] final val userBatchLookup: Quoted[EntityQuery[UserBatchLookup]] = quote {
    querySchema[UserBatchLookup](
      entity = "USER_BATCH_LOOKUP",
      _.key   -> "`key`",
      _.value -> "`value`"
    )
  }

}

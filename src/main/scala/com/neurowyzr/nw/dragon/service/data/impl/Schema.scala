package com.neurowyzr.nw.dragon.service.data.impl

import com.neurowyzr.nw.dragon.service.data.CoreSqlDbContext

import io.getquill.{EntityQuery, Quoted}

private[impl] class Schema(ctx: CoreSqlDbContext) {

  import com.neurowyzr.nw.dragon.service.data.models.*

  import ctx.*

  private[impl] final val episode: Quoted[EntityQuery[Episode]] = quote {
    querySchema[Episode](
      entity = "EPISODE",
      _.maybeSource       -> "source",
      _.maybeMessageId    -> "message_id",
      _.maybeUtcStartAt   -> "utc_start_date",
      _.maybeUtcExpiryAt  -> "utc_expiry_date",
      _.maybeUtcCreatedAt -> "utc_created_at",
      _.maybeUtcUpdatedAt -> "utc_updated_at"
    )
  }

  private[impl] final val testSession: Quoted[EntityQuery[TestSession]] = quote {
    querySchema[TestSession](
      entity = "TEST_SESSION",
      _.maybeUtcStartAt     -> "start_date",
      _.maybeUtcEndAt       -> "end_date",
      _.maybeUtcCompletedAt -> "completed_at",
      _.maybeUacRevision    -> "uac_revision",
      _.maybeFindScore      -> "find_score",
      _.maybeAggregateScore -> "aggregate_score",
      _.maybeMcqV2          -> "mcq_v2",
      _.maybeZScore         -> "z_score"
    )
  }

  private[impl] final val users: Quoted[EntityQuery[User]] = quote {
    querySchema[User](
      entity = "USERS",
      _.maybeEmailHash          -> "email",
      _.maybeLastName           -> "last_name",
      _.maybeDateOfBirth        -> "dob",
      _.maybeMobileNumber       -> "mobile_number",
      _.maybeCountry            -> "country",
      _.maybeNationality        -> "nationality",
      _.maybePostalCode         -> "postal_code",
      _.maybeAddress            -> "address",
      _.maybeGender             -> "gender",
      _.maybeUserStatus         -> "user_status",
      _.maybeUserProfile        -> "user_profile",
      _.maybeIcOrPassportNumber -> "ic_or_passport_number",
      _.maybeExternalPatientRef -> "external_patient_ref",
      _.maybeSource             -> "source",
      _.maybeUtcCreatedAt       -> "created_at",
      _.maybeUtcUpdatedAt       -> "updated_at"
    )
  }

  private[impl] final val userBatch: Quoted[EntityQuery[UserBatch]] = quote {
    querySchema[UserBatch](
      entity = "USER_BATCH",
      _.utcStartDate     -> "start_date",
      _.utcEndDate       -> "end_date",
      _.maybeDescription -> "description",
      _.maybeCode        -> "code"
    )
  }

  private[impl] final val engagements: Quoted[EntityQuery[Engagement]] = quote {
    querySchema[Engagement](
      entity = "ENGAGEMENTS",
      _.maybeNotes                   -> "notes",
      _.utcStartDate                 -> "start_date",
      _.utcEndDate                   -> "end_date",
      _.isMagicLinkEnabled           -> "enable_magic_link",
      _.maybeLocale                  -> "locale",
      _.maybeAssessmentsConfig       -> "assessments_config",
      _.maybeSubType                 -> "sub_type",
      _.maybeContentConfig           -> "content_config",
      _.maybeProductConfig           -> "product_config",
      _.maybeDecisionQuestionMapping -> "decision_question_mapping"
    )
  }

  private[impl] final val clients: Quoted[EntityQuery[Client]] = quote {
    querySchema[Client](
      entity = "CLIENTS",
      _.maybeAddress      -> "address",
      _.maybeContactPhone -> "contact_phone",
      _.maybeContactEmail -> "contact_email",
      _.maybeContactName  -> "contact_name"
    )
  }

  private[impl] final val products: Quoted[EntityQuery[Product]] = quote {
    querySchema[Product](entity = "PRODUCTS", _.maybeDescription -> "description")
  }

  private[impl] final val userAccount: Quoted[EntityQuery[UserAccount]] = quote {
    querySchema[UserAccount](
      entity = "USER_ACCOUNTS",
      _.maybeUserProfile       -> "user_profile",
      _.maybeUserAccountConfig -> "user_account_config"
    )
  }

  private[impl] final val userAccountAud: Quoted[EntityQuery[UserAccountAud]] = quote {
    querySchema[UserAccountAud](
      entity = "USER_ACCOUNTS_AUD",
      _.rev                    -> "REV",
      _.maybeRevType           -> "REVTYPE",
      _.maybeUserAccountConfig -> "user_account_config"
    )
  }

  private[impl] final val revInfo: Quoted[EntityQuery[RevInfo]] = quote {
    querySchema[RevInfo](
      entity = "REVINFO",
      _.id                        -> "REV",
      _.maybeRevTimeStampInMillis -> "REVTSTMP"
    )
  }

  private[impl] final val userRole: Quoted[EntityQuery[UserRole]] = quote {
    querySchema[UserRole](
      entity = "USER_ROLES"
    )
  }

  private[impl] final val cygnusEvent: Quoted[EntityQuery[CygnusEvent]] = quote {
    querySchema[CygnusEvent](entity = "CYGNUS_EVENT", _.maybeUtcCreatedAt -> "utc_created_at")
  }

  private[impl] final val sessionOtp: Quoted[EntityQuery[SessionOtp]] = quote {
    querySchema[SessionOtp](
      entity = "SESSION_OTP",
      _.emailHash             -> "email",
      _.maybeUtcInvalidatedAt -> "utc_invalidated_at"
    )
  }

}

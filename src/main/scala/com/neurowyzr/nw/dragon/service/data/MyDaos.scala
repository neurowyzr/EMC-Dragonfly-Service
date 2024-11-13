package com.neurowyzr.nw.dragon.service.data

import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.data.models.*
import com.neurowyzr.nw.dragon.service.data.models.Types.LongId

trait MyDaos extends SampleDao with UserDataConsentDao with UserSurveyDao with UserBatchLookupDao

private[data] sealed trait SampleDao {
  def insertSample(entity: Sample): Try[LongId]

}

private[data] sealed trait UserDataConsentDao {
  def insertConsent(entity: UserDataConsent): Try[String]
  def revokeConsentByUserId(userId: Long): Try[String]
  def getUserConsentByUserId(userId: Long): Try[Option[UserDataConsent]]

}

private[data] sealed trait UserSurveyDao {
  def insertSurveySelections(entity: UserSurvey): Try[String]

}

private[data] sealed trait UserBatchLookupDao {
  def getUserBatchLookupByKey(key: String): Try[Option[UserBatchLookup]]
  def getUserBatchLookupByCode(userBatchCode: String): Try[Option[UserBatchLookup]]

}

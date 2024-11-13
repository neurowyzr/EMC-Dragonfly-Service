package com.neurowyzr.nw.dragon.service.data

import java.time.LocalDateTime

import com.twitter.util.Try

import com.neurowyzr.nw.dragon.service.data.models.*
import com.neurowyzr.nw.dragon.service.data.models.Types.{IntId, LongId}

trait CoreDaos
    extends EpisodeDao with TestSessionDao with UserDao with UserBatchDao with EngagementDao with ClientDao
    with ProductDao with UserAccountDao with UserAccountAudDao with RevInfoDao with UserRoleDao with CygnusEventDao
    with SessionOtpDao

private[data] sealed trait EpisodeDao {
  def insertEpisodeAndTestSession(episode: Episode, testSession: TestSession): Try[(Episode, TestSession)]

  def getEpisodeById(id: Long): Try[Option[Episode]]

  def getEpisodeByTestId(testId: String): Try[Option[Episode]]

  def getEpisodeByMessageId(messageId: String): Try[Option[Episode]]

  def allEpisodes(): Try[Seq[Episode]]

  def updateEpisodeExpiryDate(id: Long, expiryDate: LocalDateTime): Try[LongId]

  def invalidateEpisode(id: LongId): Try[LongId]

  def getLatestEpisodeByUsername(username: String): Try[Option[Episode]]

  def getLatestCompletedTestSessionsByUsername(username: String): Try[Option[(Episode, TestSession)]]

  def getEpisodeByMessageIdAndSource(messageId: String, source: String): Try[Option[Episode]]

  def getEpisodeByEpisodeRefAndSource(episodeRef: String, source: String): Try[Option[Episode]]

}

private[data] sealed trait TestSessionDao {
  def insertNewTestSession(entity: TestSession): Try[Long]

  def getTestSessionsByUsernameAndUserBatch(username: String, userBatchCode: String): Try[Seq[(TestSession, String)]]

  def allTestSessions(): Try[Seq[TestSession]]

}

private[data] sealed trait UserDao {
  def insertNewUser(entity: User): Try[LongId]

  def getUserByExtPatientRef(patientRef: String): Try[Option[User]]

  def getUserBySourceAndExtPatientRef(source: String, patientRef: String): Try[Option[User]]

  def getUserByUsername(username: String): Try[Option[User]]

  def getUserById(userId: Long): Try[Option[User]]

  def updateUser(entity: User): Try[LongId]

  def deleteUserByUsername(username: String): Try[LongId]

}

private[data] sealed trait UserBatchDao {
  def insertNewUserBatch(entity: UserBatch): Try[LongId]

  def getUserBatchByCode(userBatchCode: String): Try[Option[UserBatch]]

}

private[data] sealed trait EngagementDao {
  def insertNewEngagement(entity: Engagement): Try[LongId]

  def getEngagementByUserBatchCode(userBatchCode: String): Try[Option[Engagement]]

  def allEngagements(): Try[Seq[Engagement]]

}

private[data] sealed trait ClientDao {
  def insertNewClient(entity: Client): Try[LongId]

}

private[data] sealed trait ProductDao {
  def insertNewProduct(entity: Product): Try[LongId]

}

private[data] sealed trait UserAccountDao {
  def insertNewUserAccount(entity: UserAccount): Try[LongId]

  def getUserAccountByUserIdAndUserBatchId(userId: Long, userBatchId: Long): Try[Option[UserAccount]]
}

private[data] sealed trait UserAccountAudDao {
  def insertNewUserAccountAud(entity: UserAccountAud): Try[LongId]

  def getUserAccountAudById(id: Long): Try[Seq[UserAccountAud]]
}

private[data] sealed trait RevInfoDao {
  def insertNewRevInfo(entity: RevInfo): Try[IntId]

  def getRevInfoById(id: Long): Try[Seq[RevInfo]]
}

private[data] sealed trait UserRoleDao {
  def insertNewUserRole(entity: UserRole): Try[LongId]
}

private[data] sealed trait CygnusEventDao {
  def insertNewCygnusEvent(entity: CygnusEvent): Try[LongId]

  def getCygnusEventByMessageTypeAndMessageId(messageType: String, messageId: String): Try[Option[CygnusEvent]]

}

private[data] sealed trait SessionOtpDao {
  def insertSessionOtp(userOtp: SessionOtp): Try[LongId]

  def getSessionOtp(sessionId: String, email: String): Try[Option[SessionOtp]]

  def updateSessionOtp(userOtp: SessionOtp): Try[LongId]

  def invalidateSessionOtp(sessionId: String, emailHash: String): Try[LongId]

}

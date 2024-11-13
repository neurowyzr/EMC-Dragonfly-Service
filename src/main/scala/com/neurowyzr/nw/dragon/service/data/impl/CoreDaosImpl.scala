package com.neurowyzr.nw.dragon.service.data.impl

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}

import scala.util.Random

import com.twitter.util.Try
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.data.{CoreDaos, CoreSqlDbContext}
import com.neurowyzr.nw.dragon.service.data.impl.CoreDaosImpl.{
  generateRandomString, logDelete, logInsert, logQueryOpt, logQuerySeq, logUpdate
}
import com.neurowyzr.nw.dragon.service.data.models.*
import com.neurowyzr.nw.dragon.service.data.models.Types.{IntId, LongId}

@SuppressWarnings(
  Array("org.wartremover.warts.StringPlusAny", "org.wartremover.warts.Serializable", "org.wartremover.warts.Product")
)
@Singleton
class CoreDaosImpl @Inject() (ctx: CoreSqlDbContext) extends CoreDaos with Logging {

  import ctx.*

  protected val schema = new Schema(ctx)
  protected val query  = new Queries(ctx, schema)

  // EpisodeDao
  override def insertEpisodeAndTestSession(episode: Episode, testSession: TestSession): Try[(Episode, TestSession)] = {
    val tried: Try[(Episode, TestSession)] = Try {
      ctx.transaction {
        val newTestSessionId: LongId = ctx.run(query.insertNewTestSession(testSession))
        val insertEpisode            = episode.copy(testSessionId = newTestSessionId)
        val newEpisodeId: LongId     = ctx.run(query.insertNewEpisode(insertEpisode))
        (episode.copy(id = newEpisodeId, testSessionId = newTestSessionId), testSession.copy(id = newTestSessionId))
      }
    }

    logInsert[(Episode, TestSession)](tried,
                                      "episode, test-session",
                                      { case (episode: Episode, testSession: TestSession) =>
                                        s"${episode.id.toString}, ${testSession.id.toString}"
                                      }
                                     )
  }

  override def getEpisodeById(id: Long): Try[Option[Episode]] = {
    val tried = Try(ctx.run(query.getEpisodeById(id)).headOption)
    logQueryOpt[Episode](tried, s"episode by id: $id")
  }

  override def getEpisodeByTestId(testId: String): Try[Option[Episode]] = {
    val tried = Try(ctx.run(query.getEpisodeByTestId(testId)).headOption)
    logQueryOpt[Episode](tried, s"episode by test id: $testId")
  }

  override def getEpisodeByMessageId(messageId: String): Try[Option[Episode]] = {
    val tried = Try(ctx.run(query.getEpisodeByMessageId(messageId)).headOption)
    logQueryOpt[Episode](tried, s"episode by message id: $messageId")
  }

  override def allEpisodes(): Try[Seq[Episode]] = {
    val tried: Try[List[Episode]] = Try(ctx.run(query.allEpisodes))
    logQuerySeq[Episode](tried, "all episodes")
  }

  override def updateEpisodeExpiryDate(id: Long, expiryDate: LocalDateTime): Try[LongId] = {
    val tried: Try[LongId] = Try(
      ctx.run(query.updateEpisodeExpiryDate(id, expiryDate))
    )
    logUpdate[LongId](tried, "episode", (id: LongId) => id.toString)
  }

  override def invalidateEpisode(id: LongId): Try[LongId] = {
    val tried: Try[LongId] = Try(
      ctx.run(query.invalidateEpisode(id))
    )
    logUpdate[LongId](tried, "episode", (id: LongId) => id.toString)
  }

  override def getLatestEpisodeByUsername(username: String): Try[Option[Episode]] = {
    val tried = Try(ctx.run(query.getLatestEpisodeByUsername(username)).headOption)
    logQueryOpt[Episode](tried, s"episode by username: $username")
  }

  override def getLatestCompletedTestSessionsByUsername(username: String): Try[Option[(Episode, TestSession)]] = {
    val tried: Try[Option[(Episode, TestSession)]] = Try(
      ctx.run(query.getLatestCompletedTestSessionsByEmail(username)).headOption
    )
    logQueryOpt[(Episode, TestSession)](tried, s"episode and test session by email: $username")
  }

  override def getEpisodeByMessageIdAndSource(messageId: String, source: String): Try[Option[Episode]] = {
    val tried = Try(ctx.run(query.getEpisodeByMessageIdAndSource(messageId, source)).headOption)
    logQueryOpt[Episode](tried, s"episode by message id: $messageId and source: $source")
  }

  override def getEpisodeByEpisodeRefAndSource(episodeRef: String, source: String): Try[Option[Episode]] = {
    val tried = Try(ctx.run(query.getEpisodeByEpisodeRefAndSource(episodeRef, source)).headOption)
    logQueryOpt[Episode](tried, s"episode by episode ref: $episodeRef and source: $source")
  }

  // TestSessionDao
  override def insertNewTestSession(entity: TestSession): Try[LongId] = {
    val tried: Try[LongId] = Try(ctx.run(query.insertNewTestSession(entity)))
    logInsert[LongId](tried, "test-session", (id: LongId) => id.toString)
  }

  override def getTestSessionsByUsernameAndUserBatch(username: String,
                                                     userBatchCode: String
                                                    ): Try[Seq[(TestSession, String)]] = {
    val tried: Try[List[(TestSession, String)]] = Try(
      ctx.run(query.getTestSessionsWithEpisodeRefByUsernameAndUserBatch(username, userBatchCode))
    )
    logQuerySeq[(TestSession, String)](tried, s"test-session by username $username and code: $userBatchCode")
  }

  override def allTestSessions(): Try[Seq[TestSession]] = {
    val tried = Try(ctx.run(query.allTestSessions))
    logQuerySeq[TestSession](tried, "all test-sessions")
  }

  // UserDao
  override def insertNewUser(entity: User): Try[LongId] = {
    val tried: Try[LongId] = Try(ctx.run(query.insertNewUser(entity)))
    logInsert[LongId](tried, "user", (id: LongId) => id.toString)
  }

  override def getUserByExtPatientRef(patientRef: String): Try[Option[User]] = {
    val tried = Try(ctx.run(query.getUserByExtPatientRef(patientRef)).headOption)
    logQueryOpt[User](tried, s"user by patient ref: $patientRef")
  }

  override def getUserBySourceAndExtPatientRef(source: String, patientRef: String): Try[Option[User]] = {
    val tried = Try(ctx.run(query.getUserBySourceAndExtPatientRef(source, patientRef)).headOption)
    logQueryOpt[User](tried, s"user by source: '$source' and patient ref: $patientRef")
  }

  override def getUserByUsername(username: String): Try[Option[User]] = {
    val tried = Try(ctx.run(query.getUserByUsername(username)).headOption)
    logQueryOpt[User](tried, s"user by username: '$username'")
  }

  override def getUserById(userId: Long): Try[Option[User]] = {
    val tried = Try(ctx.run(query.getUserById(userId)).headOption)
    logQueryOpt[User](tried, s"user by userId: '$userId'")
  }

  override def updateUser(entity: User): Try[LongId] = {
    val tried: Try[LongId] = Try(ctx.run(query.updateUser(entity)))
    logUpdate[LongId](tried, "user", (id: LongId) => id.toString)
  }

  // UserBatchDao
  override def insertNewUserBatch(entity: UserBatch): Try[LongId] = {
    val tried: Try[LongId] = Try(ctx.run(query.insertNewUserBatch(entity)))
    logInsert[LongId](tried, "user batch", (id: LongId) => id.toString)
  }

  override def getUserBatchByCode(userBatchCode: String): Try[Option[UserBatch]] = {
    val tried = Try(ctx.run(query.getUserBatchByCode(userBatchCode)).headOption)
    logQueryOpt[UserBatch](tried, s"user batch by code: $userBatchCode")
  }

  // EngagementDao
  override def insertNewEngagement(entity: Engagement): Try[LongId] = {
    val tried: Try[LongId] = Try(ctx.run(query.insertNewEngagement(entity)))
    logInsert[LongId](tried, "engagement", (id: LongId) => id.toString)
  }

  override def getEngagementByUserBatchCode(userBatchCode: String): Try[Option[Engagement]] = {
    val tried = Try(ctx.run(query.getEngagementByUserBatchCode(userBatchCode)).headOption)
    logQueryOpt[Engagement](tried, s"engagement by user batch code: $userBatchCode")
  }

  override def allEngagements(): Try[Seq[Engagement]] = {
    val tried: Try[List[Engagement]] = Try(ctx.run(query.allEngagements))
    logQuerySeq[Engagement](tried, "all engagements")
  }

  // ClientDao
  override def insertNewClient(entity: Client): Try[LongId] = {
    val tried: Try[LongId] = Try(ctx.run(query.insertNewClient(entity)))
    logInsert[LongId](tried, "client", (id: LongId) => id.toString)
  }

  // ProductDao
  override def insertNewProduct(entity: Product): Try[LongId] = {
    val tried: Try[LongId] = Try(ctx.run(query.insertNewProduct(entity)))
    logInsert[LongId](tried, "product", (id: LongId) => id.toString)
  }

  // UserAccountDao
  override def insertNewUserAccount(entity: UserAccount): Try[LongId] = {
    val tried: Try[LongId] = Try(ctx.run(query.insertNewUserAccount(entity)))
    logInsert[LongId](tried, "user account", (id: LongId) => id.toString)
  }

  override def getUserAccountByUserIdAndUserBatchId(userId: Long, userBatchId: Long): Try[Option[UserAccount]] = {
    val tried = Try(ctx.run(query.getUserAccountByUserIdAndUserBatchId(userId, userBatchId)).headOption)
    logQueryOpt[UserAccount](tried, s"user account by userid: '$userId' and userBatchId :'$userBatchId'")
  }

  // UserAccountAudDao
  override def insertNewUserAccountAud(entity: UserAccountAud): Try[LongId] = {
    val tried: Try[LongId] = Try(ctx.run(query.insertNewUserAccountAud(entity)))
    logInsert[LongId](tried, "user account aud", (id: LongId) => id.toString)
  }

  override def getUserAccountAudById(id: Long): Try[Seq[UserAccountAud]] = {
    val tried: Try[List[UserAccountAud]] = Try(ctx.run(query.getUserAccountAudById(id)))
    logQuerySeq[UserAccountAud](tried, s"user account aud by id: '$id'")
  }

  // RevInfoDao
  override def insertNewRevInfo(entity: RevInfo): Try[IntId] = {
    val tried: Try[IntId] = Try(ctx.run(query.insertNewRevInfo(entity)))
    logInsert[IntId](tried, "rev info", (id: IntId) => id.toString)
  }

  override def getRevInfoById(id: Long): Try[Seq[RevInfo]] = {
    val tried: Try[List[RevInfo]] = Try(ctx.run(query.getRevInfoById(id)))
    logQuerySeq[RevInfo](tried, s"rev info by id: '$id'")
  }

  // UserRoleDao
  override def insertNewUserRole(entity: UserRole): Try[LongId] = {
    val tried: Try[LongId] = Try(ctx.run(query.insertNewUserRole(entity)))
    logInsert[Long](tried,
                    s"user role by userId: '${entity.userId}' and roleId :'${entity.roleId}'",
                    (id: LongId) => id.toString
                   )
  }

  // CygnusEventDao
  override def insertNewCygnusEvent(entity: CygnusEvent): Try[LongId] = {
    val tried: Try[LongId] = Try(ctx.run(query.insertNewCygnusEvent(entity)))
    logInsert[LongId](tried, "cygnus event", (id: LongId) => id.toString)
  }

  override def getCygnusEventByMessageTypeAndMessageId(messageType: String, messageId: String): Try[Option[CygnusEvent]] = {
    val tried = Try(ctx.run(query.getCygnusEventByMessageIdAndMessageType(messageId, messageType)).headOption)
    logQueryOpt[CygnusEvent](tried, s"cygnus event by message id: '$messageId'")
  }

  // SessionOtpDao
  override def insertSessionOtp(entity: SessionOtp): Try[LongId] = {
    val tried = Try(ctx.run(query.insertSessionOtp(entity)))
    logInsert[LongId](tried, "session otp", (id: LongId) => id.toString)
  }

  override def getSessionOtp(sessionId: String, email: String): Try[Option[SessionOtp]] = {
    val tried = Try(ctx.run(query.getSessionOtp(sessionId, email)).headOption)
    logQueryOpt[SessionOtp](tried, s"session otp by session id $sessionId and email : $email")
  }

  override def updateSessionOtp(entity: SessionOtp): Try[LongId] = {
    val tried = Try(ctx.run(query.updateSessionOtp(entity)))
    logUpdate[LongId](tried, "session otp", (id: LongId) => id.toString)
  }

  override def invalidateSessionOtp(sessionId: String, emailHash: String): Try[LongId] = {
    val tried = Try(ctx.run(query.invalidateSessionOtp(sessionId, emailHash)))
    logUpdate[LongId](tried, "session otp", (id: LongId) => id.toString)
  }

  def allUsers(): Try[Seq[User]] = {
    val tried = Try(ctx.run(query.allUsers))
    logQuerySeq[User](tried, "all user")
  }

  def allSessionOtps(): Try[Seq[SessionOtp]] = {
    val tried = Try(ctx.run(query.allSessionOtps))
    logQuerySeq[SessionOtp](tried, "all session otp")
  }

  override def deleteUserByUsername(username: String): Try[LongId] = {
    val tried = Try(ctx.run(query.deleteUserByUsername(username, generateRandomString(10))))
    logDelete[LongId](tried, "user", (id: LongId) => id.toString)
  }

}

private[data] object CoreDaosImpl extends Logging {

  def logInsert[R](tried: Try[R], label: String, extractor: R => String): Try[R] = {
    tried
      .onSuccess(inserted => info(s"Created $label with id ${extractor(inserted)}"))
      .onFailure(cause => error(s"Error creating $label", cause))
  }

  def logUpdate[R](tried: Try[R], label: String, extractor: R => String): Try[R] = {
    tried
      .onSuccess(updated => info(s"Updated $label with id: '${extractor(updated)}'"))
      .onFailure(cause => error(s"Error updating $label", cause))
  }

  def logQuerySeq[R](tried: Try[Seq[R]], label: String): Try[Seq[R]] = {
    tried
      .onSuccess(records => info(s"Query for $label returned: ${records.size.toString}"))
      .onFailure(cause => error(s"Error querying for $label", cause))
  }

  def logQueryOpt[R](tried: Try[Option[R]], label: String): Try[Option[R]] = {
    tried
      .onSuccess(records => info(s"Query for $label returned: ${records.isDefined.toString}"))
      .onFailure(cause => error(s"Error querying for $label", cause))
  }

  def logDelete[R](tried: Try[R], label: String, extractor: R => String): Try[R] = {
    tried
      .onSuccess(inserted => info(s"Deleted number of $label: ${extractor(inserted)}"))
      .onFailure(cause => error(s"Error deleting $label", cause))
  }

  // used to replace fields for "deleted" users, non-reversible
  def generateRandomString(length: Int): String = {
    Random.alphanumeric.take(length).mkString
  }

}

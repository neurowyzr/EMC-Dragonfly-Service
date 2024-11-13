package com.neurowyzr.nw.dragon.service.data.impl

import java.time.{LocalDateTime, ZoneOffset}

import com.neurowyzr.nw.dragon.service.data.CoreSqlDbContext

import io.getquill.*

private[impl] class Queries(ctx: CoreSqlDbContext, schema: Schema) {

  import com.neurowyzr.nw.dragon.service.data.models.*

  import ctx.*

  // Episode
  private[impl] def insertNewEpisode(entity: Episode): Quoted[ActionReturning[Episode, Long]] = quote {
    schema.episode.insertValue(lift(entity)).returningGenerated(_.id)
  }

  private[impl] def getEpisodeById(id: Long): Quoted[Query[Episode]] = quote {
    schema.episode.filter(_.id == lift(id)).take(1)
  }

  private[impl] def getEpisodeByTestId(testId: String): Quoted[Query[Episode]] = quote {
    schema.episode.filter(_.episodeRef == lift(testId)).take(1)
  }

  private[impl] def getEpisodeByMessageId(messageId: String): Quoted[Query[Episode]] = quote {
    schema.episode.filter(_.maybeMessageId.contains(lift(messageId))).take(1)
  }

  private[impl] def allEpisodes: Quoted[EntityQuery[Episode]] = quote {
    schema.episode
  }

  private[impl] def updateEpisodeExpiryDate(id: Long, expiryDate: LocalDateTime): Quoted[Update[Episode]] = quote {
    schema.episode.filter(e => e.id == lift(id)).update(e => e.maybeUtcExpiryAt -> Some(lift(expiryDate)))
  }

  private[impl] def invalidateEpisode(id: Long): Quoted[Update[Episode]] = quote {
    schema.episode.filter(e => e.id == lift(id)).update(e => e.isInvalidated -> lift(true))
  }

  private[impl] def getLatestCompletedTestSessionsByEmail(username: String): Quoted[Query[(Episode, TestSession)]] =
    quote {
      val k: Query[(TestSession, User, Episode)] =
        for {
          testSession <- schema.testSession
          user        <- schema.users if testSession.userId == user.id
          episode     <- schema.episode if testSession.id == episode.testSessionId
        } yield (testSession, user, episode)

      k.filter { case (testSession, user, episode) =>
        user.username == lift(username) && testSession.maybeUtcCompletedAt.isDefined
      }.map { case (testSession, user, episode) => (episode, testSession) }
        .sortBy { case (episode, testSession) => testSession.maybeUtcCompletedAt }(Ord.desc)
        .take(1)
    }

  private[impl] def getEpisodeByMessageIdAndSource(messageId: String, source: String): Quoted[Query[Episode]] = quote {
    schema.episode
      .filter(_.maybeMessageId.contains(lift(messageId)))
      .filter(_.maybeSource.contains(lift(source)))
      .take(1)
  }

  private[impl] def getEpisodeByEpisodeRefAndSource(episodeRef: String, source: String): Quoted[Query[Episode]] =
    quote {
      schema.episode.filter(_.episodeRef == lift(episodeRef)).filter(_.maybeSource.contains(lift(source))).take(1)
    }

  // Test session
  private[impl] def insertNewTestSession(entity: TestSession): Quoted[ActionReturning[TestSession, Long]] = quote {
    schema.testSession.insertValue(lift(entity)).returningGenerated(_.id)
  }

  private[impl] def getTestSessionsWithEpisodeRefByUsernameAndUserBatch(username: String, userBatchCode: String) =
    quote {
      val k: Query[(TestSession, User, UserBatch, Episode)] =
        for {
          testSession <- schema.testSession
          user        <- schema.users if testSession.userId == user.id
          userBatch   <- schema.userBatch if testSession.userBatchId == userBatch.id
          episode     <- schema.episode if testSession.id == episode.testSessionId
        } yield (testSession, user, userBatch, episode)

      k.filter { case (testSession, user, userBatch, episode) =>
        testSession.maybeUtcCompletedAt.isDefined == true && user.username == lift(username) && userBatch.maybeCode
          .contains(lift(userBatchCode))
      }.map { case (testSession, user, userBatch, episode) => (testSession, episode.episodeRef) }
        .sortBy { case (testSession, _) => testSession.maybeUtcCompletedAt }(Ord.desc)
        .take(100)
    }

  private[impl] def allTestSessions: Quoted[EntityQuery[TestSession]] = quote {
    schema.testSession
  }

  // User
  private[impl] def insertNewUser(entity: User): Quoted[ActionReturning[User, Long]] = quote {
    schema.users.insertValue(lift(entity)).returningGenerated(_.id)
  }

  private[impl] def getUserByExtPatientRef(patientRef: String): Quoted[Query[User]] = quote {
    schema.users.filter(_.maybeExternalPatientRef.contains(lift(patientRef))).take(1)
  }

  private[impl] def getUserBySourceAndExtPatientRef(source: String, patientRef: String): Quoted[Query[User]] = quote {
    schema.users
      .filter(_.maybeSource.contains(lift(source)))
      .filter(_.maybeExternalPatientRef.contains(lift(patientRef)))
      .take(1)
  }

  private[impl] def getUserByUsername(username: String): Quoted[Query[User]] = quote {
    schema.users.filter(_.username == lift(username)).take(1)
  }

  private[impl] def getUserById(userId: Long): Quoted[Query[User]] = quote {
    schema.users.filter(_.id == lift(userId)).take(1)
  }

  private[impl] def updateUser(entity: User): Quoted[Update[User]] = quote {
    schema.users
      .filter(e => e.id == lift(entity.id))
      .update(
        _.username                -> lift(entity.username),
        _.maybeEmailHash          -> lift(entity.maybeEmailHash),
        _.maybeExternalPatientRef -> lift(entity.maybeExternalPatientRef),
        _.maybeUtcUpdatedAt       -> lift(entity.maybeUtcUpdatedAt),
        _.firstName               -> lift(entity.firstName),
        _.maybeDateOfBirth        -> lift(entity.maybeDateOfBirth),
        _.maybeGender             -> lift(entity.maybeGender)
      )
  }

  private[impl] def getLatestEpisodeByUsername(username: String) = quote {
    val k: Query[Episode] =
      for {
        user    <- schema.users.filter(u => u.username == lift(username))
        episode <- schema.episode if user.id == episode.userId
      } yield episode
    k.sortBy(_.maybeUtcCreatedAt)(Ord.desc)
  }

  // User Batch
  private[impl] def insertNewUserBatch(entity: UserBatch): Quoted[ActionReturning[UserBatch, Long]] = quote {
    schema.userBatch.insertValue(lift(entity)).returningGenerated(_.id)
  }

  private[impl] def getUserBatchByCode(userBatchCode: String): Quoted[Query[UserBatch]] = quote {
    schema.userBatch.filter(_.maybeCode.contains(lift(userBatchCode))).take(1)
  }

  // Engagement
  private[impl] def insertNewEngagement(entity: Engagement): Quoted[ActionReturning[Engagement, Long]] = quote {
    schema.engagements.insertValue(lift(entity)).returningGenerated(_.id)
  }

  private[impl] def getEngagementByUserBatchCode(userBatchCode: String): Quoted[Query[Engagement]] = quote {
    schema.userBatch
      .join(schema.engagements)
      .on((userBatch, engagements) => userBatch.engagementId == engagements.id)
      .withFilter { case (userBatch, engagements) => userBatch.maybeCode.contains(lift(userBatchCode)) }
      .map { case (userBatch, engagements) => engagements }
      .take(1)
  }

  private[impl] def allEngagements: Quoted[EntityQuery[Engagement]] = quote {
    schema.engagements
  }

  // Client
  private[impl] def insertNewClient(entity: Client): Quoted[ActionReturning[Client, Long]] = quote {
    schema.clients.insertValue(lift(entity)).returningGenerated(_.id)
  }

  // Product
  private[impl] def insertNewProduct(entity: Product): Quoted[ActionReturning[Product, Long]] = quote {
    schema.products.insertValue(lift(entity)).returningGenerated(_.id)
  }

  // User Account
  private[impl] def insertNewUserAccount(entity: UserAccount): Quoted[ActionReturning[UserAccount, Long]] = quote {
    schema.userAccount.insertValue(lift(entity)).returningGenerated(_.id)
  }

  private[impl] def getUserAccountByUserIdAndUserBatchId(userId: Long, userBatchId: Long): Quoted[Query[UserAccount]] =
    quote {
      schema.userAccount.filter(_.userId == lift(userId)).filter(_.userBatchId == lift(userBatchId)).take(1)
    }

  // UserAccountAudDao
  private[impl] def insertNewUserAccountAud(entity: UserAccountAud): Quoted[Insert[UserAccountAud]] = quote {
    schema.userAccountAud.insertValue(lift(entity))
  }

  private[impl] def getUserAccountAudById(id: Long): Quoted[EntityQuery[UserAccountAud]] = quote {
    schema.userAccountAud.filter(_.id == lift(id))
  }

  // RevInfoDao
  private[impl] def insertNewRevInfo(entity: RevInfo): Quoted[ActionReturning[RevInfo, Int]] = quote {
    schema.revInfo.insertValue(lift(entity)).returningGenerated(_.id)
  }

  private[impl] def getRevInfoById(id: Long): Quoted[EntityQuery[RevInfo]] = quote {
    schema.revInfo.filter(_.id == lift(id))
  }

  // User Role
  private[impl] def insertNewUserRole(entity: UserRole): Quoted[Insert[UserRole]] = quote {
    schema.userRole.insertValue(lift(entity))
  }

  // Cygnus Event
  private[impl] def insertNewCygnusEvent(entity: CygnusEvent): Quoted[ActionReturning[CygnusEvent, Long]] = quote {
    schema.cygnusEvent.insertValue(lift(entity)).returningGenerated(_.id)
  }

  private[impl] def getCygnusEventByMessageIdAndMessageType(messageId: String, messageType: String) = quote {
    schema.cygnusEvent.filter(_.messageId == lift(messageId)).filter(_.messageType == lift(messageType)).take(1)
  }

  // Session OTP
  private[impl] def insertSessionOtp(entity: SessionOtp): Quoted[ActionReturning[SessionOtp, Long]] = quote {
    schema.sessionOtp.insertValue(lift(entity)).returningGenerated(_.id)
  }

  private[impl] def getSessionOtp(sessionId: String, email: String): Quoted[Query[SessionOtp]] = quote {
    schema.sessionOtp.filter(e => e.sessionId == lift(sessionId)).filter(e => e.emailHash == lift(email)).take(1)
  }

  private[impl] def updateSessionOtp(entity: SessionOtp): Quoted[Update[SessionOtp]] = quote {
    schema.sessionOtp
      .filter(e => e.id == lift(entity.id))
      .update(
        _.emailHash             -> lift(entity.emailHash),
        _.otpValue              -> lift(entity.otpValue),
        _.attemptCount          -> lift(entity.attemptCount),
        _.utcCreatedAt          -> lift(entity.utcCreatedAt),
        _.utcExpiredAt          -> lift(entity.utcExpiredAt),
        _.maybeUtcInvalidatedAt -> lift(entity.maybeUtcInvalidatedAt)
      )
  }

  private[impl] def invalidateSessionOtp(sessionId: String, emailHash: String): Quoted[Update[SessionOtp]] = quote {
    schema.sessionOtp
      .filter(e => e.sessionId == lift(sessionId))
      .filter(e => e.emailHash == lift(emailHash))
      .filter(e => e.maybeUtcInvalidatedAt == lift(Option.empty[LocalDateTime]))
      .update(
        _.maybeUtcInvalidatedAt -> lift(Option(LocalDateTime.now(ZoneOffset.UTC)))
      )
  }

  private[impl] def deleteUserByUsername(username: String, masked: String): Quoted[Update[User]] = quote {
    schema.users
      .filter(e => e.username == lift(username))
      .update(
        _.username       -> lift(s"deleted_$masked"),
        _.maybeEmailHash -> lift(Option.empty[String]),
        _.firstName      -> lift(s"deleted_$masked")
      )
  }

  // Non exposed methods meant for unit testing
  private[impl] def allUsers: Quoted[EntityQuery[User]] = quote {
    schema.users
  }

  private[impl] def allUserAccounts: Quoted[EntityQuery[UserAccount]] = quote {
    schema.userAccount
  }

  private[impl] def allSessionOtps: Quoted[EntityQuery[SessionOtp]] = quote {
    schema.sessionOtp
  }

}

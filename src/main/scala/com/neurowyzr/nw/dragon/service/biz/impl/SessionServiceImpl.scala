package com.neurowyzr.nw.dragon.service.biz.impl

import java.time.*
import java.util.Date
import javax.inject.Inject

import scala.util.Random

import com.twitter.finagle.http.Response
import com.twitter.util.{Future, Throw, Try}
import com.twitter.util.jackson.ScalaObjectMapper

import com.neurowyzr.nw.dragon.service.biz.SessionService
import com.neurowyzr.nw.dragon.service.biz.exceptions.*
import com.neurowyzr.nw.dragon.service.biz.impl.SessionServiceImpl.*
import com.neurowyzr.nw.dragon.service.biz.models.*
import com.neurowyzr.nw.dragon.service.biz.models.Defaults.DefaultUserRole
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.*
import com.neurowyzr.nw.dragon.service.biz.models.UserSurveyModels.{CreateUserSurveyParams, UserSurvey}
import com.neurowyzr.nw.dragon.service.cfg.Models.{AppTimezone, DbfsConfig}
import com.neurowyzr.nw.dragon.service.clients.CoreHttpClient
import com.neurowyzr.nw.dragon.service.data.*
import com.neurowyzr.nw.dragon.service.mq.{EmailPublisher, SelfPublisher}
import com.neurowyzr.nw.dragon.service.utils.context.{EncryptionUtil, JwtUtil}

import com.github.t3hnar.bcrypt.*
import com.google.inject.Singleton
import io.scalaland.chimney.dsl.TransformationOps

@Singleton
class SessionServiceImpl @Inject() (engagementRepo: EngagementRepository,
                                    userBatchRepo: UserBatchRepository,
                                    episodeRepo: EpisodeRepository,
                                    userRepository: UserRepository,
                                    userAccountRepo: UserAccountRepository,
                                    revInfoRepository: RevInfoRepository,
                                    userAccountAudRepo: UserAccountAudRepository,
                                    userRoleRepo: UserRoleRepository,
                                    sessionOtpRepository: SessionOtpRepository,
                                    dbfsConfig: DbfsConfig,
                                    emailPublisher: EmailPublisher,
                                    selfPublisher: SelfPublisher,
                                    coreHttpClient: CoreHttpClient,
                                    userSurveyRepository: UserSurveyRepository,
                                    mapper: ScalaObjectMapper,
                                    appTimezone: AppTimezone
                                   )
    extends SessionService {

  def validatePatientIdAndEpisodeId(params: EnqueueTestSessionParams): Try[Unit] = {
    if (params.patientId != params.patientRef) {
      Throw(BizException(s"Expecting patient id: ${params.patientId} but received ${params.patientRef}."))
    } else if (params.episodeId != params.episodeRef) {
      Throw(BizException(s"Expecting episode id: ${params.episodeId} but received ${params.episodeRef}."))
    } else {
      Try.Unit
    }
  }

  override def enqueueTestSession(params: EnqueueTestSessionParams): Future[String] = {
    Future.const(validatePatientIdAndEpisodeId(params)).flatMap { _ =>
      selfPublisher.publishTestSessionMessage(params)
    }
  }

  override def createSession(params: CreateSessionParams): Future[String] = {
    val sessionId     = params.sessionId
    val userBatchCode = params.userBatchCode
    val email         = params.email
    userBatchRepo.getUserBatchByCode(userBatchCode).flatMap {
      case Some(userBatch) =>
        val userBatchId = userBatch.id
        engagementRepo.getEngagementByUserBatchCode(userBatchCode).flatMap {
          case Some(engagement) =>
            val engagementId = engagement.id
            userRepository.getUserByUsername(email).flatMap {
              case Some(user) =>
                val userId         = user.id
                val newEpisode     = sessionEpisode(userId, sessionId, appTimezone)
                val newTestSession = TestSession(userId, userBatchId, engagementId)
                createDependencyDataForRepeatedUser(userId, userBatchId).flatMap { _ =>
                  createEpisodeAndTestSession(engagementId, userBatchId, userId, sessionId)
                }
              case None =>
                val reason = s"User not found for email: $email"
                Future.exception(BizException(reason))
            }
          case _ =>
            val reason = s"Engagement doesn't exist for this user batch code: $userBatchCode"
            Future.exception(BizException(reason))
        }
      case _ =>
        val reason = s"User batch code: $userBatchCode not found."
        Future.exception(BizException(reason))
    }
  }

  override def createUserAndSession(params: CreateUserSessionParams): Future[String] = {
    val sessionId     = params.sessionId
    val userBatchCode = params.userBatchCode
    userBatchRepo.getUserBatchByCode(userBatchCode).flatMap {
      case Some(userBatch) =>
        val userBatchId = userBatch.id
        engagementRepo.getEngagementByUserBatchCode(userBatchCode).flatMap {
          case Some(engagement) =>
            val engagementId = engagement.id
            val newUser      = createNewUser(params, dbfsConfig.newUserDefaultPassword.value)
            userRepository.createUser(newUser).flatMap { user =>
              val userId = user.id
              createDependencyDataForNewUser(userId, userBatchId).flatMap { _ =>
                createEpisodeAndTestSession(engagementId, userBatchId, userId, sessionId)
              }
            }
          case _ =>
            val reason = s"Engagement doesn't exist for this user batch code: $userBatchCode"
            Future.exception(BizException(reason))
        }
      case _ =>
        val reason = s"User batch code: $userBatchCode not found."
        Future.exception(BizException(reason))
    }
  }

  override def updateSession(params: UpdateSessionParams): Future[Unit] = {
    val sessionId = params.sessionId
    val email     = params.email

    fetchUserWithIncompleteSignup(sessionId, email).flatMap { user =>
      insertOrUpdateSessionOtp(sessionId, email, user)
    }
  }

  override def verifySession(params: VerifySessionParams): Future[String] = {
    val sessionId = params.sessionId
    val email     = params.email
    val otpValue  = params.otp
    val name      = params.name

    fetchUserWithIncompleteSignup(sessionId, email).flatMap { user =>
      sessionOtpRepository.getSessionOtp(sessionId, EncryptionUtil.aesEncrypt(email)).flatMap {
        case Some(sessionOtp) =>
          verifyOtp(sessionId, email, otpValue, sessionOtp).flatMap { _ =>
            val jwt = JwtUtil.generateToken(sessionId, email, name)
            Future.value(jwt)
          }
        case None =>
          val reason = s"User with this session '$sessionId' and email '$email' not found."
          Future.exception(SessionNotFoundException(reason))
      }
    }
  }

  override def login(params: LoginParams): Future[Unit] = {
    val sessionId = params.loginSessionId
    val email     = params.email

    userRepository.getUserByUsername(email).flatMap {
      case Some(user) => insertOrUpdateSessionOtp(sessionId, email, user)
      case None =>
        val reason = s"Username '$email' is missing"
        Future.exception(UserNotFoundException(reason))
    }
  }

  override def verifyLogin(params: VerifyLoginParams): Future[String] = {
    val sessionId = params.loginSessionId
    val email     = params.email
    val otpValue  = params.otp

    sessionOtpRepository.getSessionOtp(sessionId, EncryptionUtil.aesEncrypt(email)).flatMap {
      case Some(sessionOtp) =>
        verifyOtp(sessionId, email, otpValue, sessionOtp).flatMap { _ =>
          findUserAndGenerateJWT(email, sessionId)
        }
      case None =>
        val reason = s"User with this session '$sessionId' and email '$email' not found."
        Future.exception(SessionNotFoundException(reason))
    }
  }

  override def sendUserReport(params: SendUserReport): Future[Response] = {
    coreHttpClient.sendUserReport(params)
  }

  override def createUserSurvey(params: CreateUserSurveyParams): Future[Unit] = {
    userRepository.getUserByUsername(params.username).flatMap {
      case Some(user) =>
        val stringSurvey = mapper.writeValueAsString(params.surveyItems)
        val survey =
          params
            .into[UserSurvey]
            .withFieldConst(_.userId, user.id)
            .withFieldConst(_.utcCreatedAt, LocalDateTime.now(ZoneOffset.UTC))
            .withFieldConst(_.surveySelections, stringSurvey)
            .transform
        userSurveyRepository.createUserSurvey(survey).map(_ => ())
      case None =>
        val reason = s"User '${params.sessionId}' does not exist"
        Future.exception(UserNotFoundException(reason))
    }
  }

  override def getLatestCompletedSession(params: LatestCompletedSessionParams): Future[LatestTestSession] = {
    val email = params.email
    episodeRepo.getLatestCompletedTestSessionsByUsername(email).flatMap {
      case Some((episode, testSession)) =>
        val sessionId = episode.episodeRef
        testSession.maybeZScore match {
          case Some(_) => Future.value(LatestTestSession(sessionId, isScoreReady = true))
          case None    => Future.value(LatestTestSession(sessionId, isScoreReady = false))
        }
      case None =>
        val reason = s"Test session not found for email: $email"
        Future.exception(SessionNotFoundException(reason))
    }
  }

  override def isNewSessionAllowedByUserName(params: CheckNewSessionAllowedParams): Future[Boolean] = {
    episodeRepo.getLatestEpisodeByUsername(params.username).map {
      case Some(episode) =>
        episode.maybeUtcStartAt
          .map { utcStartAt =>
            !isSameDay(utcStartAt)
          }
          .getOrElse(true)
      case None => true
    }
  }

  override def getSessionOtp(sessionId: String, email: String): Future[Option[SessionOtp]] = {
    sessionOtpRepository.getSessionOtp(sessionId, EncryptionUtil.aesEncrypt(email))
  }

  override def invalidateSessionOtp(sessionId: String, email: String): Future[Unit] = {
    sessionOtpRepository.invalidateSessionOtp(sessionId, EncryptionUtil.aesEncrypt(email)).flatMap(_ => Future.Unit)
  }

  // To be compatible with existing cognifyx-core assessment retrieval,
  // USER_ACCOUNTS table must contain 1 record for first and repeated user,
  // which containing the relationship between user_id and user_batch_id
  // USER_ACCOUNTS_AUD must contain 1 record for first and repeated user,
  // For repeated user, up to 2 entries are needed with 2nd record having this field
  // {"FREQUENCY": "DAILY"} in user_account_config column
  // Therefore we segregate this behaviour in 2 methods namely
  // createDependencyDataForNewUser & createDependencyDataForRepeatedUser

  def createDependencyDataForNewUser(userId: Long, userBatchId: Long): Future[Unit] = {
    val newUserRole = UserRole(userId, DefaultUserRole)
    userRoleRepo.createUserRole(newUserRole).flatMap { _ =>
      val newUserAccount = UserAccount(userId, userBatchId)
      userAccountRepo.createUserAccount(newUserAccount).flatMap { userAccount =>
        val newRevInfo = RevInfo(Some(Instant.now().toEpochMilli))
        revInfoRepository.createRevInfo(newRevInfo).flatMap { revInfo =>
          val userAccountId     = userAccount.id
          val revId             = revInfo.id
          val newUserAccountAud = UserAccountAud.firstEntry(userAccountId, revId)
          userAccountAudRepo.createUserAccountAud(newUserAccountAud).flatMap { _ =>
            Future.Unit
          }
        }
      }
    }
  }

  def createDependencyDataForRepeatedUser(userId: Long, userBatchId: Long): Future[Unit] = {
    userAccountRepo.getUserAccountByUserIdAndUserBatchId(userId, userBatchId).flatMap {
      case Some(userAccount) =>
        val userAccountId = userAccount.id
        userAccountAudRepo.getUserAccountAudById(userAccountId).flatMap {
          case Seq(first) =>
            val newRevInfo = RevInfo(Some(Instant.now().toEpochMilli))
            revInfoRepository.createRevInfo(newRevInfo).flatMap { revInfo =>
              val userAccountId     = userAccount.id
              val revId             = revInfo.id
              val newUserAccountAud = UserAccountAud.lastEntry(userAccountId, revId)
              userAccountAudRepo.createUserAccountAud(newUserAccountAud).flatMap { _ =>
                Future.Unit
              }
            }
          case Seq(first, second) => Future.Unit

          case _ =>
            val reason =
              "User account aud is empty or more than 2 entries for repeated user " +
                s"with userId '${userId.toString}' and userBatchId '${userBatchId.toString}'. "
            Future.exception(BizException(reason))
        }
      case None =>
        val reason =
          s"User account entry not found for userId '${userId.toString}' and userBatchId '${userBatchId.toString}'. "
        Future.exception(BizException(reason))
    }
  }

  def createEpisodeAndTestSession(engagementId: Long, userBatchId: Long, userId: Long, sessionId: String): Future[String] = {
    val currentTestSession = CurrentTestSession(engagementId, userBatchId, userId)
    coreHttpClient
      .getCurrentTestSession(currentTestSession)
      .flatMap { testSessionDetail =>
        val newEpisode     = sessionEpisode(userId, sessionId, appTimezone)
        val newTestSession = TestSession(userId, userBatchId, engagementId, testSessionDetail.testSessionOrder)
        episodeRepo
          .insertEpisodeAndTestSession(newEpisode, newTestSession)
          .map(_ => dbfsConfig.magicLinkPath + "/" + sessionId)
          .rescue { case _: BizException =>
            val reason = s"Failed to create test session for sessionId $sessionId."
            Future.exception(BizException(reason))
          }
      }
      .rescue { case e: BizException => Future.exception(BizException(e.getMessage)) }
  }

  private def fetchUserWithIncompleteSignup(sessionId: String, email: String): Future[User] = {
    userRepository.getUserByUsername(email).flatMap {
      case Some(_) =>
        val reason = s"User with this email '$email' already exist"
        Future.exception(UserExistsException(reason))
      case None =>
        userRepository.getUserByUsername(sessionId).flatMap {
          case Some(user) => Future.value(user)
          case None =>
            val reason = s"User with this session '$sessionId' is missing"
            Future.exception(UserNotFoundException(reason))
        }
    }
  }

  private def insertOrUpdateSessionOtp(sessionId: String, email: String, user: User): Future[Unit] = {
    val newSessionOtp = SessionOtp(sessionId,
                                   EncryptionUtil.aesEncrypt(email),
                                   generateOTP,
                                   otpExpiryDateTime(dbfsConfig.otpValidityInMinutes)
                                  )

    sessionOtpRepository.getSessionOtp(sessionId, EncryptionUtil.aesEncrypt(email)).flatMap {
      case Some(sessionOtp) =>
        val updatedSessionOtp = newSessionOtp.copy(id = sessionOtp.id)
        sessionOtpRepository.updateSessionOtp(updatedSessionOtp).flatMap { _ =>
          val emailArgs = EmailOtpArgs(updatedSessionOtp.otpValue, user.firstName)
          emailPublisher.publishOtpEmail(emailArgs, Set(email))
          Future.Unit
        }
      case None =>
        sessionOtpRepository.insertSessionOtp(newSessionOtp).flatMap { sessionOtp =>
          val emailArgs = EmailOtpArgs(sessionOtp.otpValue, user.firstName)
          emailPublisher.publishOtpEmail(emailArgs, Set(email))
          Future.Unit
        }
    }
  }

  private def verifyOtp(sessionId: String, email: String, otpValue: String, sessionOtp: SessionOtp): Future[Unit] = {
    if (isExpired(sessionOtp.utcExpiredAt)) {
      val reason = s"User with this session '$sessionId', email '$email', otp has expired."
      Future.exception(OtpExpiredException(reason))
    } else if (sessionOtp.attemptCount >= dbfsConfig.otpRetries) {
      val reason = s"User with this session '$sessionId', email '$email' has exceeded no of tries."
      Future.exception(OtpTriesExceededException(reason))
    } else {
      val totalAttempts = sessionOtp.attemptCount + 1
      sessionOtpRepository.updateSessionOtp(sessionOtp.copy(attemptCount = totalAttempts))
      if (sessionOtp.otpValue != otpValue) {
        val reason = s"User with this session '$sessionId', email '$email' and otp '$otpValue' is incorrect."
        Future.exception(OtpMismatchException(reason))
      } else {
        Future.Unit
      }
    }
  }

  private def findUserAndGenerateJWT(email: String, sessionId: String): Future[String] = {
    userRepository.getUserByUsername(email).flatMap { maybeUser =>
      maybeUser
        .map { user =>
          val jwt = JwtUtil.generateToken(sessionId, email, user.firstName)
          Future.value(jwt)
        }
        .getOrElse {
          val reason = s"Username '$email' is missing"
          Future.exception(UserNotFoundException(reason))
        }
    }
  }

  /** Compares whether the given date is the same date as the current date in the app's timezone
    * @param utcDate
    * @return
    */
  private def isSameDay(utcDate: LocalDateTime): Boolean = {
    val appZoneId = ZoneId.of(appTimezone)
    val now       = LocalDateTime.now().atZone(appZoneId).toLocalDateTime
    val zonedDate = utcDate.atZone(ZoneOffset.UTC).withZoneSameInstant(appZoneId).toLocalDateTime
    zonedDate.getYear == now.getYear &&
    zonedDate.getMonth == now.getMonth &&
    zonedDate.getDayOfMonth == now.getDayOfMonth
  }

}

private[impl] object SessionServiceImpl {

  def createNewUser(params: CreateUserSessionParams, defaultPassword: String): User = {
    val sessionId = params.sessionId
    User(
      username = sessionId,
      password = generatePasswordHash(defaultPassword),
      firstName = sessionId,
      source = sessionId.take(30),
      externalPatientRef = sessionId,
      country = params.countryCode
    )
  }

  def generatePasswordHash(password: String): String = {
    val salt           = generateSalt
    val hashedPassword = password.bcryptBounded(salt)

    hashedPassword
  }

  def generateOTP: String = {
    // Generate a random 6-digit number
    val otp = Random.nextInt(900000) + 100000
    otp.toString
  }

  def otpExpiryDateTime(validityPeriod: Int): LocalDateTime = {
    LocalDateTime.now.plusMinutes(validityPeriod.longValue)
  }

  def isExpired(dateTime: LocalDateTime): Boolean = {
    val currentDateTime = LocalDateTime.now(ZoneOffset.UTC)
    dateTime.isBefore(currentDateTime)
  }

  def sessionEpisode(userId: Long, sessionId: String, appTimezone: String): Episode = {
    Episode(
      userId = userId,
      episodeRef = sessionId,
      messageId = sessionId,
      source = sessionId.take(50),
      utcStartAt = LocalDateTime.now(ZoneOffset.UTC),
      utcExpiryAt = getUtcLocalEndOfDay(appTimezone)
    )
  }

  def getUtcLocalEndOfDay(appTimezone: AppTimezone): LocalDateTime = {
    val appZoneId                = ZoneId.of(appTimezone)
    val appEndOfDay              = LocalDateTime.of(LocalDate.now(appZoneId), LocalTime.MAX)
    val appEndOfDayZonedDateTime = appEndOfDay.atZone(appZoneId)

    val utcEndOfDayZonedDateTime = appEndOfDayZonedDateTime.withZoneSameInstant(ZoneOffset.UTC)
    utcEndOfDayZonedDateTime.toLocalDateTime
  }

}

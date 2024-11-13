package com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages

import java.time.{Instant, LocalDateTime}
import javax.inject.Inject

import com.twitter.util.Future

import com.neurowyzr.nw.dragon.service.biz.exceptions.{BizException, CreateTestSessionException}
import com.neurowyzr.nw.dragon.service.biz.impl.pipelines.one.stages.CreateTestSessionStage.clientEpisode
import com.neurowyzr.nw.dragon.service.biz.models.{
  CreateTestSessionTask, Episode, RevInfo, TestSession, UserAccount, UserAccountAud, UserRole
}
import com.neurowyzr.nw.dragon.service.biz.models.CreateTestSessionStatus.TestSessionCreationFailure
import com.neurowyzr.nw.dragon.service.biz.models.Defaults.DefaultUserRole
import com.neurowyzr.nw.dragon.service.biz.models.Outcomes.{PatientRefExist, PatientRefNotFound, Success}
import com.neurowyzr.nw.dragon.service.biz.models.SessionModels.CurrentTestSession
import com.neurowyzr.nw.dragon.service.cfg.Models.{CustomerConfig, DbfsConfig}
import com.neurowyzr.nw.dragon.service.clients.CoreHttpClient
import com.neurowyzr.nw.dragon.service.data.{
  EpisodeRepository, RevInfoRepository, UserAccountAudRepository, UserAccountRepository, UserRoleRepository
}
import com.neurowyzr.nw.finatra.lib.pipeline.FStage

import com.softwaremill.quicklens.ModifyPimp

private[impl] class CreateTestSessionStage @Inject() (
    userRoleRepo: UserRoleRepository,
    userAccountRepo: UserAccountRepository,
    revInfoRepo: RevInfoRepository,
    userAccountAudRepo: UserAccountAudRepository,
    episodeRepo: EpisodeRepository,
    dbfsConfig: DbfsConfig,
    customerConfig: CustomerConfig,
    coreHttpClient: CoreHttpClient
)() extends FStage[CreateTestSessionTask] {

  override def execute(task: CreateTestSessionTask): Future[CreateTestSessionTask] = {
    val requestId    = task.in.requestId
    val episodeRef   = task.in.episodeRef
    val startDate    = task.in.startDate
    val expiryDate   = task.in.expiryDate
    val userId       = task.out.maybeUserId.get
    val userBatchId  = task.out.maybeUserBatchId.get
    val engagementId = task.out.maybeEngagementId.get
    info("notify test session being called!!")
    task.out.maybeOutcome.get match {
      case PatientRefExist => // repeated user
        createDependencyDataForRepeatedUser(userId, userBatchId)
          .flatMap { _ =>
            createEpisodeAndTestSession(engagementId, userBatchId, userId, episodeRef, requestId, startDate, expiryDate)
          }
          .map(magicLinkUrl =>
            task.modify(_.out.maybeMagicLinkUrl).setTo(Some(magicLinkUrl)).modify(_.out.maybeOutcome).setTo(Some(Success))
          )
          .rescue { case e: Exception => abort(CreateTestSessionException(TestSessionCreationFailure, e.getMessage)) }

      case PatientRefNotFound => // new user
        createDependencyDataForNewUser(userId, userBatchId)
          .flatMap { _ =>
            createEpisodeAndTestSession(engagementId, userBatchId, userId, episodeRef, requestId, startDate, expiryDate)
          }
          .map(magicLinkUrl =>
            task.modify(_.out.maybeMagicLinkUrl).setTo(Some(magicLinkUrl)).modify(_.out.maybeOutcome).setTo(Some(Success))
          )
          .rescue { case e: Exception => abort(CreateTestSessionException(TestSessionCreationFailure, e.getMessage)) }
    }

  }

  private def createEpisodeAndTestSession(engagementId: Long,
                                          userBatchId: Long,
                                          userId: Long,
                                          sessionId: String,
                                          requestId: String,
                                          startDate: LocalDateTime,
                                          expiryDate: LocalDateTime
                                         ): Future[String] = {
    val currentTestSession = CurrentTestSession(engagementId, userBatchId, userId)
    coreHttpClient.getCurrentTestSession(currentTestSession).flatMap { testSessionDetail =>
      val newEpisode     = clientEpisode(userId, sessionId, requestId, customerConfig.source, startDate, expiryDate)
      val newTestSession = TestSession(userId, userBatchId, engagementId, testSessionDetail.testSessionOrder)
      episodeRepo
        .insertEpisodeAndTestSession(newEpisode, newTestSession)
        .map(_ => dbfsConfig.magicLinkPath + "/" + sessionId)
    }
  }

  private def createDependencyDataForNewUser(userId: Long, userBatchId: Long): Future[Unit] = {
    val newUserRole = UserRole(userId, DefaultUserRole)
    userRoleRepo.createUserRole(newUserRole).flatMap { _ =>
      val newUserAccount = UserAccount(userId, userBatchId)
      userAccountRepo.createUserAccount(newUserAccount).flatMap { userAccount =>
        val newRevInfo = RevInfo(Some(Instant.now().toEpochMilli))
        revInfoRepo.createRevInfo(newRevInfo).flatMap { revInfo =>
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

  private def createDependencyDataForRepeatedUser(userId: Long, userBatchId: Long): Future[Unit] = {
    userAccountRepo.getUserAccountByUserIdAndUserBatchId(userId, userBatchId).flatMap {
      case Some(userAccount) =>
        val userAccountId = userAccount.id
        userAccountAudRepo.getUserAccountAudById(userAccountId).flatMap {
          case Seq(first) =>
            val newRevInfo = RevInfo(Some(Instant.now().toEpochMilli))
            revInfoRepo.createRevInfo(newRevInfo).flatMap { revInfo =>
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
            Future.exception(CreateTestSessionException(TestSessionCreationFailure, reason))
        }
      case None =>
        val reason =
          s"User account entry not found for userId '${userId.toString}' and userBatchId '${userBatchId.toString}'. "
        Future.exception(CreateTestSessionException(TestSessionCreationFailure, reason))
    }
  }

}

private object CreateTestSessionStage {

  def clientEpisode(userId: Long,
                    sessionId: String,
                    requestId: String,
                    source: String,
                    startDate: LocalDateTime,
                    expiryDate: LocalDateTime
                   ): Episode = {
    Episode(
      userId = userId,
      episodeRef = sessionId,
      messageId = requestId,
      source = source,
      utcStartAt = startDate,
      utcExpiryAt = expiryDate
    )
  }

}

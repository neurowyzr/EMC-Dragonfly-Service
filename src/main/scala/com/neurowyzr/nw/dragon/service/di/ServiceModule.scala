package com.neurowyzr.nw.dragon.service.di

import javax.inject.Singleton

import com.twitter.inject.TwitterModule
import com.twitter.util.jackson.ScalaObjectMapper

import com.neurowyzr.nw.dragon.service.biz.{ReportService, ScoreService, SessionService, UserService}
import com.neurowyzr.nw.dragon.service.biz.impl.{
  ReportServiceImpl, ScoreServiceImpl, SessionServiceImpl, UserServiceImpl
}
import com.neurowyzr.nw.dragon.service.cfg.Models.{AppTimezone, DbfsConfig}
import com.neurowyzr.nw.dragon.service.clients.CoreHttpClient
import com.neurowyzr.nw.dragon.service.data.*
import com.neurowyzr.nw.dragon.service.mq.{EmailPublisher, SelfPublisher}

import com.google.inject.{Module, Provides}

object ServiceModule extends TwitterModule {

  override def modules: Seq[Module] = Seq(
    ConfigModule,
    ClientModule,
    DataModule
  )

  @Provides
  @Singleton
  def providesScoreService(repo: TestSessionRepository): ScoreService = {
    new ScoreServiceImpl(repo)
  }

  @Provides
  @Singleton
  def providesReportService(dbfsConfig: DbfsConfig): ReportService = {
    new ReportServiceImpl(dbfsConfig)
  }

  @Provides
  @Singleton
  def providesSessionService(engagementRepo: EngagementRepository,
                             userBatchRepo: UserBatchRepository,
                             episodeRepo: EpisodeRepository,
                             userRepository: UserRepository,
                             userAccountRepo: UserAccountRepository,
                             userAccountAudRepo: UserAccountAudRepository,
                             revInfoRepository: RevInfoRepository,
                             userRoleRepo: UserRoleRepository,
                             sessionOtpRepository: SessionOtpRepository,
                             dbfsConfig: DbfsConfig,
                             emailPublisher: EmailPublisher,
                             selfPublisher: SelfPublisher,
                             coreHttpClient: CoreHttpClient,
                             userSurveyRepository: UserSurveyRepository,
                             mapper: ScalaObjectMapper,
                             appTimezone: AppTimezone
                            ): SessionService = {
    new SessionServiceImpl(
      engagementRepo,
      userBatchRepo,
      episodeRepo,
      userRepository,
      userAccountRepo,
      revInfoRepository,
      userAccountAudRepo,
      userRoleRepo,
      sessionOtpRepository,
      dbfsConfig,
      emailPublisher,
      selfPublisher,
      coreHttpClient,
      userSurveyRepository,
      mapper,
      appTimezone
    )
  }

  @Provides
  @Singleton
  def providesUserService(userRepository: UserRepository,
                          userDataConsentRepository: UserDataConsentRepository,
                          coreHttpClient: CoreHttpClient,
                          episodeRepository: EpisodeRepository
                         ): UserService = {
    new UserServiceImpl(userRepository, userDataConsentRepository, coreHttpClient, episodeRepository)
  }

}

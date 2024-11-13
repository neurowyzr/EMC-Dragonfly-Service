package com.neurowyzr.nw.dragon.service.di

import javax.inject.Singleton

import com.twitter.inject.TwitterModule
import com.twitter.util.FuturePool

import com.neurowyzr.nw.dragon.service.data.*
import com.neurowyzr.nw.dragon.service.data.impl.*
import com.neurowyzr.nw.dragon.service.di.annotations.{CoreDatabaseContext, DragonflyDatabaseContext}

import com.google.inject.{Module, Provides}

object DataModule extends TwitterModule {

  override val modules: Seq[Module] = Seq(
    DatabaseContextModule
  )

  @Provides
  @Singleton
  def providesAllDaos(@CoreDatabaseContext ctx: CoreSqlDbContext): CoreDaos = {
    new CoreDaosImpl(ctx)
  }

  @Provides
  @Singleton
  def providesAllDaosExtra(@DragonflyDatabaseContext ctx: DragonSqlDbContext): MyDaos = {
    new MyDaosImpl(ctx)
  }

  @Provides
  @Singleton
  def providesEpisodeRepository(daos: CoreDaos, fp: FuturePool): EpisodeRepository = {
    new EpisodeRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesUserAccountRepository(daos: CoreDaos, fp: FuturePool): UserAccountRepository = {
    new UserAccountRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesUserAccountAudRepository(daos: CoreDaos, fp: FuturePool): UserAccountAudRepository = {
    new UserAccountAudRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesRevInfoRepository(daos: CoreDaos, fp: FuturePool): RevInfoRepository = {
    new RevInfoRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesUserRoleRepository(daos: CoreDaos, fp: FuturePool): UserRoleRepository = {
    new UserRoleRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesUserRepository(daos: CoreDaos, fp: FuturePool): UserRepository = {
    new UserRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesUserDataConsentRepository(daos: MyDaos, fp: FuturePool): UserDataConsentRepository = {
    new UserDataConsentRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesUserSurveyRepository(daos: MyDaos, fp: FuturePool): UserSurveyRepository = {
    new UserSurveyRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesUserBatchRepository(daos: CoreDaos, fp: FuturePool): UserBatchRepository = {
    new UserBatchRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesTestSessionRepository(daos: CoreDaos, fp: FuturePool): TestSessionRepository = {
    new TestSessionRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesEngagementRepository(daos: CoreDaos, fp: FuturePool): EngagementRepository = {
    new EngagementRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesCygnusEventRepository(daos: CoreDaos, fp: FuturePool): CygnusEventRepository = {
    new CygnusEventRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesSessionOtpRepository(daos: CoreDaos, fp: FuturePool): SessionOtpRepository = {
    new SessionOtpRepositoryImpl(daos, fp)
  }

  @Provides
  @Singleton
  def providesUserBatchLookupRepository(daos: MyDaos, fp: FuturePool): UserBatchLookupRepository = {
    new UserBatchLookupRepositoryImpl(daos, fp)
  }

}

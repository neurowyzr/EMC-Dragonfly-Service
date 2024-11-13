package com.neurowyzr.nw.dragon.service.di

import javax.inject.Singleton

import com.twitter.inject.TwitterModule

import com.neurowyzr.nw.dragon.service.biz.impl.ApiKeyBasedAuthServiceImpl
import com.neurowyzr.nw.dragon.service.cfg.Models.CustomerConfig
import com.neurowyzr.nw.finatra.lib.biz.ApiKeyBasedAuthService

import com.google.inject.{Module, Provides}

object AuthModule extends TwitterModule {

  override def modules: Seq[Module] = Seq(
    ConfigModule
  )

  @Provides
  @Singleton
  def providesApiKeyBasedAuthService(customerConfig: CustomerConfig): ApiKeyBasedAuthService = {
    new ApiKeyBasedAuthServiceImpl(customerConfig)
  }

}

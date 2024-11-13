package com.neurowyzr.nw.dragon.service.di

import javax.inject.Singleton

import com.twitter.finagle.http.Request
import com.twitter.inject.TwitterModule

import com.neurowyzr.nw.finatra.lib.api.impl.{CustomLogFormatter, CustomLogFormatterImpl}

import com.google.inject.Provides

object MiscModule extends TwitterModule {

  @Provides
  @Singleton
  def providesCustomLogFormatter(): CustomLogFormatter[Request] = {
    new CustomLogFormatterImpl()
  }

}

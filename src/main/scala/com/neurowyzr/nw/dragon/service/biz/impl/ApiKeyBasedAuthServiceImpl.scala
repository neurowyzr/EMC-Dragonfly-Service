package com.neurowyzr.nw.dragon.service.biz.impl

import javax.inject.Inject

import com.twitter.util.Future
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.cfg.Models.CustomerConfig
import com.neurowyzr.nw.finatra.lib.api.filters.AuthApiKeyHandler
import com.neurowyzr.nw.finatra.lib.api.filters.impl.DefaultAuthApiKeyHandler
import com.neurowyzr.nw.finatra.lib.biz.ApiKeyBasedAuthService
import com.neurowyzr.nw.finatra.lib.biz.exceptions.AuthenticationException

import com.google.inject.Singleton

@Singleton
class ApiKeyBasedAuthServiceImpl @Inject() (customerConfig: CustomerConfig)
    extends ApiKeyBasedAuthService with Logging {

  private val authTokenHandler = DefaultAuthApiKeyHandler()

  override def tokenHandler: AuthApiKeyHandler = authTokenHandler

  override def authenticate(apiKey: String): Future[Map[String, String]] = {
    apiKey match {
      case customerConfig.`nwApiKey` => Future.value(Map.empty)
      case _                         => Future.exception(AuthenticationException(s"The API key '$apiKey' is invalid!"))
    }

  }

}

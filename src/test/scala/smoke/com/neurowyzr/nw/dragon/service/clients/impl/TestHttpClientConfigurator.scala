package com.neurowyzr.nw.dragon.service.clients.impl

import javax.inject.Inject

import com.twitter.finagle.Http

import com.neurowyzr.nw.finatra.lib.clients.HttpClientConfigurator

import com.google.inject.Singleton

@Singleton
class TestHttpClientConfigurator @Inject() () extends HttpClientConfigurator {

  // disables TLS
  override def configureHttpClient(client: Http.Client, hostname: String): Http.Client = {
    client
  }

}

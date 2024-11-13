package com.neurowyzr.nw.dragon.service

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

import com.google.inject.Stage

class ServerAppStartupTest extends FeatureTest {

  private lazy val testServer =
    new EmbeddedHttpServer(
      twitterServer = new ServerApp,
      stage = Stage.PRODUCTION,
      disableTestLogging = true
    )

  override protected def server: EmbeddedHttpServer = testServer

  test("Server#startup") {
    // this test checks that the dependency injection is working as expected
    server.assertHealthy()
  }

}

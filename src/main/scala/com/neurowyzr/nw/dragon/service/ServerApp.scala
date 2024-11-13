package com.neurowyzr.nw.dragon.service

import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.annotations.Flags

import com.neurowyzr.nw.dragon.service.ServerApp.{CorsAllowHeaders, CorsAllowMethods, CorsExposedHeaders}
import com.neurowyzr.nw.dragon.service.api.{
  BizExceptionMapper, FakeCustomerController, ReportController, ScoreController, SessionController, UsersController
}
import com.neurowyzr.nw.dragon.service.api.filters.JwtFilterService
import com.neurowyzr.nw.dragon.service.di.*
import com.neurowyzr.nw.finatra.lib.DefaultHttpServer
import com.neurowyzr.nw.finatra.lib.api.controllers.{CorsController, WhoamiController}
import com.neurowyzr.nw.finatra.lib.api.filters.*
import com.neurowyzr.nw.finatra.lib.api.mappers.AuthenticationExceptionMapper

import com.google.inject.Module

class ServerApp extends DefaultHttpServer {

  // must not hold reference to a flag
  flag(name = "cors.origin", default = "", help = "Override for CORS origin."): Unit

  override def modules: Seq[Module] = Seq(
    ConfigModule,
    MigrationModule,
    ClientModule,
    MiscModule,
    MqModule,
    DataModule,
    ServiceModule,
    AuthModule
  )

  override def configureHttp(router: HttpRouter): Unit = {
    val corsOrigin = injector.instance[String](Flags.named("cors.origin"))
    val _ = router
      .filter[DefaultFilterSet1]
      .filter(DefaultCorsFilter(corsOrigin, CorsAllowMethods, CorsAllowHeaders, CorsExposedHeaders))
      .filter[DefaultFilterSet2]
      .filter[DefaultFilterSet3]
      .exceptionMapper[AuthenticationExceptionMapper]
      .exceptionMapper[BizExceptionMapper]
      .add[CorsController]
      .add[WhoamiController]
      .add[ScoreController]
      .add[JwtFilterService, ReportController]
      .add[SessionController]
      .add[UsersController]
//      .add[FakeCustomerController]
  }

}

private object ServerApp {

  private final val CorsAllowMethods: String = "GET, POST, PUT, DELETE"

  private final val CorsAllowHeaders: String =
    "Accept, Authorization, Content-Type, User-Agent, X-App-Version, X-Request-ID, X-Requested-With"

  private final val CorsExposedHeaders: String = DefaultCorsFilter.ResponseHeaders

}

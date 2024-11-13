package com.neurowyzr.nw.dragon.service.api

import javax.inject.Inject

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.annotations.RouteParam
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.api.ReportController.GetReportRequest
import com.neurowyzr.nw.dragon.service.api.filters.JwtFilterService
import com.neurowyzr.nw.dragon.service.biz.ReportService

class ReportController @Inject() (service: ReportService) extends Controller with Logging {

  filter[JwtFilterService].get("/v1/report/:session_id") { (request: GetReportRequest) =>
    response.ok(service.getReportPath(request.sessionId))
  }

}

private object ReportController {

  final case class GetReportRequest(
      @RouteParam("session_id") sessionId: String,
      underlying: Request
  )

}

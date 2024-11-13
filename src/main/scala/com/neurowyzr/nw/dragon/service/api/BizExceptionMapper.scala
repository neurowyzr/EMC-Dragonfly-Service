package com.neurowyzr.nw.dragon.service.api

import javax.inject.Inject

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.util.logging.Logging

import com.neurowyzr.nw.dragon.service.biz.exceptions.BizException

import com.google.inject.Singleton

@Singleton
class BizExceptionMapper @Inject() (response: ResponseBuilder) extends ExceptionMapper[BizException] with Logging {

  override def toResponse(request: Request, exception: BizException): Response = {
    error("Request failed, reason: " + exception.getMessage + System.lineSeparator() + request.contentString, exception)
    response.badRequest(exception.getMessage)
  }

}
